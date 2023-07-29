package ru.joke.memcache.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public final class XmlConfigurationSource implements ConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(XmlConfigurationSource.class);

    private static final String SCHEMA_PATH = "/configuration-schema/configuration.xsd";

    private static final String CACHE_ELEMENT = "cache";
    private static final String EXPIRED_CLEANING_POOL_SIZE_ATTR = "expired-cleaning-pool-size";
    private static final String CACHE_NAME_ATTR = "name";
    private static final String CACHE_EVICTION_POLICY_ELEMENT = "eviction-policy";
    private static final String CACHE_STORE_ELEMENT = "store";
    private static final String CACHE_EVENT_LISTENERS_ELEMENT = "event-listeners";
    private static final String CACHE_EVENT_LISTENER_CLS_ELEMENT = "class";
    private static final String CACHE_EXPIRATION_ELEMENT = "expiration";
    private static final String CACHE_STORE_MAX_ELEMENTS_ATTR = "max-elements";
    private static final String CACHE_STORE_CONCURRENCY_LEVEL_ATTR = "concurrency-level";
    private static final String CACHE_EXPIRATION_TTL_ATTR = "ttl";
    private static final String CACHE_EXPIRATION_IDLE_TTL_ATTR = "idle-ttl";
    private static final String CACHE_EXPIRATION_ETERNAL_ATTR = "eternal";

    private final boolean enableAutoDetection;
    private final Set<ConfigurationLoader> configurationLoaders;
    private final AutoDetectedConfigurationFilesCollector autoDetectedConfigurationFilesCollector;

    private XmlConfigurationSource(
            final boolean enableAutoDetection,
            final Set<ConfigurationLoader> configurationLoaders) {
        this.enableAutoDetection = enableAutoDetection;
        this.configurationLoaders = configurationLoaders;
        this.autoDetectedConfigurationFilesCollector = new AutoDetectedConfigurationFilesCollector();
    }

    @Nonnull
    @Override
    public Configuration pull() {

        logger.debug("Pull configurations from xml was called: {}", this);

        final Set<ConfigurationLoader> loaders = new HashSet<>(this.configurationLoaders);
        if (this.enableAutoDetection) {
            final Set<File> autoDetectedConfigs = this.autoDetectedConfigurationFilesCollector.collect();
            autoDetectedConfigs
                    .stream()
                    .map(FileConfigurationLoader::new)
                    .forEach(loaders::add);
        }

        validateConfigurations(loaders);
        logger.debug("Configurations validated");

        final Set<Configuration> configurations =
                this.configurationLoaders
                        .stream()
                        .map(this::pullOneConfiguration)
                        .collect(Collectors.toSet());

        final Set<CacheConfiguration> collectedConfigs = new HashSet<>();
        int expiredCleaningPoolSizeMax = 1;
        for (Configuration configuration : configurations) {

            collectedConfigs.addAll(configuration.cacheConfigurations());
            if (configuration.cleaningPoolSize() > expiredCleaningPoolSizeMax) {
                expiredCleaningPoolSizeMax = configuration.cleaningPoolSize();
            }
        }

        return composeConfiguration(collectedConfigs, expiredCleaningPoolSizeMax);
    }

    @Override
    public String toString() {
        return "XmlConfigurationSource{" +
                "enableAutoDetection=" + enableAutoDetection +
                ", configurationLoaders=" + configurationLoaders +
                '}';
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    private Configuration pullOneConfiguration(final ConfigurationLoader configurationLoader) {

        logger.debug("Pull called for loader: {}", configurationLoader);
        try (final InputStream xmlStream = configurationLoader.openStream()) {

            final Document doc = parseConfiguration(xmlStream);

            return buildConfigurationsFromDocument(doc);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            logger.error("Unable to parse configuration from xml", ex);
            throw new RuntimeException(ex);
        }
    }

    private Configuration buildConfigurationsFromDocument(final Document document) {

        final Set<CacheConfiguration> result = new HashSet<>();

        final int cleaningPoolSize = Integer.parseInt(document.getDocumentElement().getAttribute(EXPIRED_CLEANING_POOL_SIZE_ATTR));
        final NodeList caches = document.getElementsByTagName(CACHE_ELEMENT);
        for (int cacheIndex = 0; cacheIndex < caches.getLength(); cacheIndex++) {

            final Node node = caches.item(cacheIndex);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            final Element cacheElement = (Element) node;
            final String cacheName = cacheElement.getAttribute(CACHE_NAME_ATTR);

            final List<CacheEntryEventListener<?, ?>> eventListeners = parseEventListeners(cacheElement);
            final EvictionPolicy evictionPolicy = parseEvictionPolicy(cacheElement);
            final ExpirationConfiguration expirationConfiguration = createExpirationConfiguration(cacheElement);
            final StoreConfiguration storeConfiguration = createStoreConfiguration(cacheElement);

            final CacheConfiguration cacheConfiguration =
                    CacheConfiguration.
                            builder()
                                .setCacheName(cacheName)
                                .setEvictionPolicy(evictionPolicy)
                                .setExpirationConfiguration(expirationConfiguration)
                                .setStoreConfiguration(storeConfiguration)
                                .setCacheEntryEventListeners(eventListeners)
                            .build();
            result.add(cacheConfiguration);
        }

        logger.debug("Configuration was build: {}", result);

        return composeConfiguration(Set.copyOf(result), cleaningPoolSize);
    }

    private StoreConfiguration createStoreConfiguration(final Element cacheElement) {

        final NodeList storeConfigNode = cacheElement.getElementsByTagName(CACHE_STORE_ELEMENT);
        final Element storeConfigElement = (Element) storeConfigNode.item(0);

        final String maxElementsStr = storeConfigElement.getAttribute(CACHE_STORE_MAX_ELEMENTS_ATTR);
        final String concurrencyLevelStr = storeConfigElement.getAttribute(CACHE_STORE_CONCURRENCY_LEVEL_ATTR);

        return new ImmutableStoreConfiguration(
                Integer.parseInt(concurrencyLevelStr),
                Long.parseLong(maxElementsStr)
        );
    }

    private ExpirationConfiguration createExpirationConfiguration(final Element cacheElement) {

        final NodeList expirationConfigNode = cacheElement.getElementsByTagName(CACHE_EXPIRATION_ELEMENT);
        final Element expirationConfigElement = (Element) expirationConfigNode.item(0);

        final String eternalStr = expirationConfigElement.getAttribute(CACHE_EXPIRATION_ETERNAL_ATTR);
        final String ttlStr = expirationConfigElement.getAttribute(CACHE_EXPIRATION_TTL_ATTR);
        final String idleTtlStr = expirationConfigElement.getAttribute(CACHE_EXPIRATION_IDLE_TTL_ATTR);

        final boolean eternal = Boolean.parseBoolean(eternalStr);
        return new ImmutableExpirationConfiguration(
                eternal ? -1 : Long.parseLong(idleTtlStr),
                eternal ? -1 : Long.parseLong(ttlStr)
        );
    }

    private EvictionPolicy parseEvictionPolicy(final Element cacheElement) {

        final NodeList policyNode = cacheElement.getElementsByTagName(CACHE_EVICTION_POLICY_ELEMENT);
        final Element policyElement = (Element) policyNode.item(0);
        return EvictionPolicy.valueOf(policyElement.getTextContent());
    }

    private List<CacheEntryEventListener<?, ?>> parseEventListeners(final Element cacheElement) {

        final NodeList eventListeners = cacheElement.getElementsByTagName(CACHE_EVENT_LISTENERS_ELEMENT);
        if (eventListeners.getLength() == 0) {
            return Collections.emptyList();
        }

        final List<CacheEntryEventListener<?, ?>> result = new ArrayList<>();

        final Element listenersElement = (Element) eventListeners.item(0);
        final NodeList listenersClasses = listenersElement.getElementsByTagName(CACHE_EVENT_LISTENER_CLS_ELEMENT);

        for (int listenerIndex = 0; listenerIndex < listenersClasses.getLength(); listenerIndex++) {
            final Node listenerClass = listenersClasses.item(listenerIndex);
            result.add(createEventListenerInstance(listenerClass.getTextContent()));
        }

        return result;
    }

    private CacheEntryEventListener<?, ?> createEventListenerInstance(final String className) {
        try {
            @SuppressWarnings("unchecked")
            final Class<CacheEntryEventListener<?, ?>> listenerClass = (Class<CacheEntryEventListener<?, ?>>) Class.forName(className);
            return listenerClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Document parseConfiguration(final InputStream configurationStream) throws ParserConfigurationException, IOException, SAXException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        final DocumentBuilder builder = factory.newDocumentBuilder();

        final Document doc = builder.parse(configurationStream);
        doc.getDocumentElement().normalize();

        return doc;
    }

    private static InputStream openInputStream(final String configurationFilePath) {
        return XmlConfigurationSource.class.getResourceAsStream(configurationFilePath);
    }

    private void validateConfigurations(final Set<ConfigurationLoader> configurationLoaders) {
        try (final InputStream xsdStream = openInputStream(SCHEMA_PATH)) {
            final Validator validator = createValidator(xsdStream);

            for (final ConfigurationLoader configurationLoader : configurationLoaders) {
                try (final InputStream xmlStream = configurationLoader.openStream()) {
                    validator.validate(new StreamSource(xmlStream));
                } catch (SAXException | IOException ex) {
                    logger.error("Invalid configuration provided: " + configurationLoader, ex);
                    throw new RuntimeException("Invalid configuration file: " + configurationLoader, ex);
                }
            }

        } catch (SAXException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Validator createValidator(final InputStream xsdStream) throws SAXException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Source schemaFile = new StreamSource(xsdStream);
        final Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    private Configuration composeConfiguration(final Set<CacheConfiguration> cacheConfigurations, final int cleaningPoolSize) {
        return new Configuration() {
            @Override
            public Set<CacheConfiguration> cacheConfigurations() {
                return cacheConfigurations;
            }

            @Override
            public int cleaningPoolSize() {
                return cleaningPoolSize;
            }
        };
    }

    public static class Builder {

        private boolean enableAutoDetection;
        private final Set<ConfigurationLoader> configurationLoaders = new HashSet<>();

        @Nonnull
        public Builder withAutoDetectionEnabled(final boolean autoDetectionEnabled) {
            this.enableAutoDetection = autoDetectionEnabled;
            return this;
        }

        @Nonnull
        public Builder addExternalConfigurationFile(@Nonnull final File configurationFile) {
            this.configurationLoaders.add(new FileConfigurationLoader(configurationFile));
            return this;
        }

        @Nonnull
        public Builder addExternalConfigurationFiles(@Nonnull final Set<File> configurationFiles) {
            configurationFiles
                    .stream()
                    .map(FileConfigurationLoader::new)
                    .forEach(this.configurationLoaders::add);
            return this;
        }

        @Nonnull
        public Builder addResourceConfigurationFilePath(@Nonnull final String resourceConfigurationFilePath) {
            this.configurationLoaders.add(new ConfigurationLoaderByResourcePath(resourceConfigurationFilePath));
            return this;
        }

        @Nonnull
        public Builder addResourceConfigurationFilePaths(@Nonnull final Set<String> resourceConfigurationFilePaths) {
            resourceConfigurationFilePaths
                    .stream()
                    .map(ConfigurationLoaderByResourcePath::new)
                    .forEach(this.configurationLoaders::add);
            return this;
        }

        @Nonnull
        public ConfigurationSource build() {
            return new XmlConfigurationSource(this.enableAutoDetection, this.configurationLoaders);
        }
    }

    private abstract static class ConfigurationLoader {

        abstract InputStream openStream() throws FileNotFoundException;
    }

    private static class FileConfigurationLoader extends ConfigurationLoader {

        private final File file;

        private FileConfigurationLoader(final File file) {
            this.file = file;
        }

        @Override
        InputStream openStream() throws FileNotFoundException {
            return new FileInputStream(this.file);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FileConfigurationLoader that = (FileConfigurationLoader) o;
            return file.equals(that.file);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public String toString() {
            return "FileConfigurationLoader{" +
                    "file=" + file +
                    '}';
        }
    }

    private static class ConfigurationLoaderByResourcePath extends ConfigurationLoader {

        private final String resourcePath;

        private ConfigurationLoaderByResourcePath(final String resourcePath) {
            this.resourcePath = resourcePath;
        }

        @Override
        InputStream openStream() {
            return openInputStream(this.resourcePath);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ConfigurationLoaderByResourcePath that = (ConfigurationLoaderByResourcePath) o;
            return resourcePath.equals(that.resourcePath);
        }

        @Override
        public int hashCode() {
            return resourcePath.hashCode();
        }

        @Override
        public String toString() {
            return "ConfigurationLoaderByResourcePath{" +
                    "resourcePath='" + resourcePath + '\'' +
                    '}';
        }
    }
}
