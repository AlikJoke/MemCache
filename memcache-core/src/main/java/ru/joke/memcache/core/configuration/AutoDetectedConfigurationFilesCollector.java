package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@ThreadSafe
final class AutoDetectedConfigurationFilesCollector {

    private static final String JAR_EXTENSION = ".jar";
    private static final String XML_EXTENSION = ".xml";
    private static final String AUTO_SCAN_CONFIG_DIR = "/memcache/";

    @Nonnull
    Set<File> collect() {

        final ClassLoader classLoader = AutoDetectedConfigurationFilesCollector.class.getClassLoader();

        if (!(classLoader instanceof URLClassLoader)) {
            throw new ClassCastException("Auto detection of configuration files failed: ClassLoader isn't " + URLClassLoader.class);
        }

        final Set<File> result = new HashSet<>();

        final URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (final URL url : urls) {
            if (url.getPath().endsWith(JAR_EXTENSION)) {
                continue;
            }

            final String jarFilePath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);

            try (final JarFile jarFile = new JarFile(jarFilePath)) {

                jarFile.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().startsWith(AUTO_SCAN_CONFIG_DIR))
                        .filter(entry -> entry.getName().endsWith(XML_EXTENSION))
                        .map(entry -> createFileFromJarEntry(jarFile, entry))
                        .forEach(result::add);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private File createFileFromJarEntry(final JarFile jar, final JarEntry entry) {

        final File outputFile = createTempFile(entry.getName());
        try (jar;
             final InputStream inputStream = jar.getInputStream(entry);
             final FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputFile;
    }

    private File createTempFile(final String fileName) {
        try {
            final File outputFile = File.createTempFile(fileName, XML_EXTENSION);
            outputFile.deleteOnExit();

            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
