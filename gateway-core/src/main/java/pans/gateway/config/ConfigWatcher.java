package pans.gateway.config;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration file watcher
 * Only watches files that exist in the file system
 */
public class ConfigWatcher {

    private static final Logger log = LoggerFactory.getLogger(ConfigWatcher.class);

    private final Vertx vertx;
    private final Path configPath;
    private final ConfigReloader reloader;
    private final AtomicLong lastModified = new AtomicLong(0);
    private long timerId;
    private final long checkIntervalMs;
    private final boolean watchingEnabled;

    public ConfigWatcher(Vertx vertx, String configPath, ConfigReloader reloader) {
        this(vertx, configPath, reloader, 1000); // Check every second
    }

    public ConfigWatcher(Vertx vertx, String configPath, ConfigReloader reloader, long checkIntervalMs) {
        this.vertx = vertx;
        this.configPath = Paths.get(configPath);
        this.reloader = reloader;
        this.checkIntervalMs = checkIntervalMs;

        // Check if file exists in file system
        this.watchingEnabled = Files.exists(this.configPath);

        if (watchingEnabled) {
            // Initialize last modified time
            updateLastModified();
            log.info("File watching enabled for: {}", this.configPath);
        } else {
            log.info("Configuration file not found in file system, file watching disabled: {} (loaded from classpath)", configPath);
        }
    }

    public void start() {
        if (!watchingEnabled) {
            log.info("Config watcher not started (file watching disabled for classpath resources)");
            return;
        }

        log.info("Starting config watcher for: {}", configPath);

        timerId = vertx.setPeriodic(checkIntervalMs, id -> {
            vertx.executeBlocking(() -> {
                checkForUpdates();
                return null;
            }, false);
        });

        log.info("Config watcher started, checking every {}ms", checkIntervalMs);
    }

    public void stop() {
        if (timerId != 0) {
            vertx.cancelTimer(timerId);
            timerId = 0;
            log.info("Config watcher stopped");
        }
    }

    private void checkForUpdates() {
        try {
            long currentModified = java.nio.file.Files.getLastModifiedTime(configPath).toMillis();

            if (currentModified > lastModified.get()) {
                log.info("Configuration file modified: {}", configPath);
                lastModified.set(currentModified);
                reloadConfig();
            }
        } catch (Exception e) {
            log.error("Error checking configuration file: ", e);
        }
    }

    private void reloadConfig() {
        try {
            log.info("Reloading configuration from: {}", configPath);
            reloader.reload(configPath.toString());
        } catch (Exception e) {
            log.error("Failed to reload configuration: {}", e.getMessage(), e);
        }
    }

    private void updateLastModified() {
        try {
            long modified = java.nio.file.Files.getLastModifiedTime(configPath).toMillis();
            lastModified.set(modified);
        } catch (Exception e) {
            log.warn("Could not get initial file modification time: {}", e.getMessage());
        }
    }
}
