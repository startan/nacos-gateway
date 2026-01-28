package nextf.nacos.gateway.config;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.reader.ConfigFileReader;

/**
 * Configuration file watcher
 * Responsibility: Encapsulate ConfigFileReader's listening mechanism to bridge callbacks and hot reload
 */
public class ConfigWatcher {

    private static final Logger log = LoggerFactory.getLogger(ConfigWatcher.class);

    private final ConfigFileReader configFileReader;
    private final ConfigReloader reloader;
    private volatile boolean isStarted = false;

    public ConfigWatcher(ConfigFileReader configFileReader, ConfigReloader reloader) {
        this.configFileReader = configFileReader;
        this.reloader = reloader;
    }

    public void start() {
        if (isStarted) {
            log.warn("ConfigWatcher already started");
            return;
        }

        log.info("Starting config watcher for: {}", configFileReader.getSourceDescription());

        // Register listening callback through ConfigFileReader
        configFileReader.watchConfig(() -> {
            log.info("Configuration change detected, triggering reload...");
            reloader.reload();
        });

        isStarted = true;
        log.info("ConfigWatcher started successfully");
    }

    public void stop() {
        if (!isStarted) {
            return;
        }

        log.info("Stopping config watcher...");
        configFileReader.stopWatching();
        isStarted = false;
        log.info("ConfigWatcher stopped");
    }
}
