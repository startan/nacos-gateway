package nextf.nacos.gateway.config.reader;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration file reader factory
 * Returns corresponding reader instance based on configuration path protocol
 */
public class ConfigFileReaderFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigFileReaderFactory.class);

    /**
     * Get corresponding reader based on configuration path
     * @param configPath Configuration path (supports file://, classpath://, nacos:// protocols)
     * @param vertx Vertx instance (required by file protocol)
     * @return ConfigFileReader instance
     */
    public static ConfigFileReader getReader(String configPath, Vertx vertx) throws Exception {
        if (configPath == null || configPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Config path cannot be null or empty");
        }

        log.info("Creating config reader for: {}", configPath);

        // Determine protocol type
        if (configPath.startsWith("file://")) {
            return new FileConfigReader(vertx, configPath);

        } else if (configPath.startsWith("classpath://")) {
            return new ClasspathConfigReader(configPath);

        } else if (configPath.startsWith("nacos://")) {
            return new NacosConfigReader(configPath);

        } else {
            // Default to file:// protocol (backward compatible)
            log.debug("No protocol specified, defaulting to file:// protocol");
            return new FileConfigReader(vertx, "file://" + configPath);
        }
    }
}
