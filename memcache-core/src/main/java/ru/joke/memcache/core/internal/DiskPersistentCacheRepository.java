package ru.joke.memcache.core.internal;

import one.nio.serial.ObjectInputChannel;
import one.nio.serial.ObjectOutputChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.PersistentStoreConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ThreadSafe
final class DiskPersistentCacheRepository implements PersistentCacheRepository {

    private static final Logger logger = LoggerFactory.getLogger(DiskPersistentCacheRepository.class);

    private static final String TMP_DIR_PROPERTY = "java.io.tmpdir";
    private static final String USER_HOME_DIR_PROPERTY = "user.home";
    private static final String USER_DIR_PROPERTY = "user.dir";

    private static final String MEM_CACHE_DIR = "memcache";
    private static final String STORE_EXTENSION = ".bin";

    private final PersistentStoreConfiguration persistentConfiguration;
    private final EntryMetadataFactory metadataFactory;
    private final File dataStore;

    DiskPersistentCacheRepository(
            @Nonnull CacheConfiguration cacheConfiguration,
            @Nonnull EntryMetadataFactory metadataFactory) {
        this.metadataFactory = metadataFactory;
        this.persistentConfiguration = cacheConfiguration.persistentStoreConfiguration().orElseThrow();
        this.dataStore =
                this.persistentConfiguration.location() == null
                    ? createDataFile(cacheConfiguration.cacheName())
                    : resolveStoreByPath(cacheConfiguration.cacheName());
    }

    @Override
    public synchronized  <K extends Serializable, V extends Serializable> void save(@Nonnull Collection<MemCacheEntry<K, V>> entries) {

        try (final FileChannel fileChannel = openDataStoreFileWriteChannel();
             final ObjectOutput out = new ObjectOutputChannel(fileChannel)) {
            writeTo(out, entries);
        } catch (IOException ex) {
            logger.error("Unable to serialize entries", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    @Nonnull
    public synchronized <K extends Serializable, V extends Serializable> Collection<MemCacheEntry<K, V>> load() {

        if (!this.dataStore.exists()) {
            return Collections.emptySet();
        }

        try (final FileChannel fileChannel = openDataStoreFileReadChannel();
             final ObjectInput in = new ObjectInputChannel(fileChannel)) {

            if (in.available() < 0) {
                return Collections.emptySet();
            }

            final int capacity = in.readInt();
            final Set<MemCacheEntry<K, V>> entries = new HashSet<>(capacity, 1);

            while (in.available() > 0) {
                final byte keyType = in.readByte();
                @SuppressWarnings("unchecked")
                final K key = (K) (keyType == 1 ? in.readUTF() : in.readObject());

                final EntryMetadata<?, K> metadata = this.metadataFactory.create(key);
                metadata.restoreMetadata(in);

                @SuppressWarnings("unchecked")
                final V value = (V) in.readObject();

                entries.add(new MemCacheEntry<>(value, metadata));
            }

            return entries;
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Unable to deserialize entries", ex);
            return Collections.emptySet();
        } finally {
            deleteDataStoreIfPossible();
        }
    }

    private void deleteDataStoreIfPossible() {
        try {
            Files.deleteIfExists(this.dataStore.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File resolveStoreByPath(final String cacheName) {
        final String dataLocationPath = this.persistentConfiguration.location();
        final String targetDirPath =
                dataLocationPath
                    .replace(escapeProperty(TMP_DIR_PROPERTY), System.getProperty(TMP_DIR_PROPERTY))
                    .replace(escapeProperty(USER_DIR_PROPERTY), System.getProperty(USER_DIR_PROPERTY))
                    .replace(escapeProperty(USER_HOME_DIR_PROPERTY), System.getProperty(USER_HOME_DIR_PROPERTY));
        final File uniqueStoreDir = new File(targetDirPath, this.persistentConfiguration.uid());
        return new File(uniqueStoreDir, cacheName + STORE_EXTENSION);
    }

    private String escapeProperty(final String property) {
        return "${" + property + "}";
    }

    private File createDataFile(final String cacheName) {
        final String tempDirPath = System.getProperty(TMP_DIR_PROPERTY);
        final File memCacheDir = new File(tempDirPath, MEM_CACHE_DIR);
        final File uniqueStoreDir = new File(memCacheDir, this.persistentConfiguration.uid());
        return new File(uniqueStoreDir, cacheName + STORE_EXTENSION);
    }

    private FileChannel openDataStoreFileWriteChannel() throws IOException {
        return FileChannel.open(
                this.dataStore.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND
        );
    }

    private FileChannel openDataStoreFileReadChannel() throws IOException {
        return FileChannel.open(
                this.dataStore.toPath(),
                StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        );
    }

    private <K extends Serializable, V extends Serializable> void writeTo(
            final ObjectOutput output,
            final Collection<MemCacheEntry<K, V>> entries) throws IOException {

        output.writeInt(entries.size());
        for (final MemCacheEntry<?, ?> entry : entries) {
            // A small optimization for strings: the vast majority of cache keys are strings. readUTF is more efficient than readObject for strings.
            final Object key = entry.metadata().key();
            final int keyType = getKeyType(key);
            output.writeByte(keyType);

            if (keyType == 1) {
                output.writeUTF(key.toString());
            } else {
                output.writeObject(key);
            }

            entry.metadata().storeMetadata(output);
            output.writeObject(entry.value());
        }
    }

    private int getKeyType(final Object key) {
        return key.getClass() == String.class ? 1 : 2;
    }
}
