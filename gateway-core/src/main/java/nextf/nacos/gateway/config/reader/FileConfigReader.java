package nextf.nacos.gateway.config.reader;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件系统配置读取器
 * 从本地文件系统读取配置，支持文件变更监听
 */
public class FileConfigReader implements ConfigFileReader {

    private static final Logger log = LoggerFactory.getLogger(FileConfigReader.class);

    private final Vertx vertx;
    private final Path configPath;
    private final AtomicLong lastModified = new AtomicLong(0);
    private long timerId;
    private final long checkIntervalMs;

    public FileConfigReader(Vertx vertx, String configPath) {
        this(vertx, configPath, 1000); // 默认1秒检查间隔
    }

    public FileConfigReader(Vertx vertx, String configPath, long checkIntervalMs) {
        this.vertx = vertx;
        this.configPath = Path.of(removeProtocolPrefix(configPath));
        this.checkIntervalMs = checkIntervalMs;

        // 初始化最后修改时间
        if (Files.exists(this.configPath)) {
            updateLastModified();
        }
    }

    @Override
    public String readConfig() throws IOException {
        log.debug("Reading config from file: {}", configPath);

        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        return Files.readString(configPath);
    }

    @Override
    public void watchConfig(Runnable callback) {
        log.info("Starting file watcher for: {}", configPath);

        timerId = vertx.setPeriodic(checkIntervalMs, id -> {
            vertx.executeBlocking(() -> {
                checkForUpdates(callback);
                return null;
            }, false);
        });

        log.info("File watcher started, checking every {}ms", checkIntervalMs);
    }

    @Override
    public void stopWatching() {
        if (timerId != 0) {
            vertx.cancelTimer(timerId);
            timerId = 0;
            log.info("File watcher stopped");
        }
    }

    @Override
    public String getSourceDescription() {
        return "file://" + configPath;
    }

    private void checkForUpdates(Runnable callback) {
        try {
            long currentModified = Files.getLastModifiedTime(configPath).toMillis();

            if (currentModified > lastModified.get()) {
                log.info("Configuration file modified: {}", configPath);
                lastModified.set(currentModified);
                callback.run();
            }
        } catch (Exception e) {
            log.error("Error checking configuration file: {}", e.getMessage(), e);
        }
    }

    private void updateLastModified() {
        try {
            long modified = Files.getLastModifiedTime(configPath).toMillis();
            lastModified.set(modified);
        } catch (Exception e) {
            log.warn("Could not get initial file modification time: {}", e.getMessage());
        }
    }

    private String removeProtocolPrefix(String path) {
        if (path.startsWith("file://")) {
            return path.substring(7);
        }
        return path;
    }
}
