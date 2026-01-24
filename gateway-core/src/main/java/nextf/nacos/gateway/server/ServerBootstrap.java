package nextf.nacos.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.ConfigLoader;
import nextf.nacos.gateway.config.ConfigReloader;
import nextf.nacos.gateway.config.ConfigWatcher;
import nextf.nacos.gateway.config.GatewayConfig;

/**
 * Server bootstrap
 * Manages the GatewayServerManager which handles multiple gateway servers
 */
public class ServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    private final String configPath;
    private Vertx vertx;
    private GatewayServerManager gatewayServerManager;
    private ConfigWatcher configWatcher;

    public ServerBootstrap(String configPath) {
        this.configPath = configPath;
    }

    public void start() throws Exception {
        // Load configuration
        ConfigLoader configLoader = new ConfigLoader();
        GatewayConfig config = configLoader.load(configPath);

        // Create Vert.x instance
        vertx = Vertx.vertx();

        // Create and start gateway server manager (manages multiple servers)
        gatewayServerManager = new GatewayServerManager(vertx, config, configPath);
        gatewayServerManager.start();

        // Create config reloader with registry and shared components from GatewayServerManager
        ConfigReloader reloader = new ConfigReloader(
                configLoader,
                gatewayServerManager.getRegistry(),
                gatewayServerManager.getConnectionManager(),
                gatewayServerManager.getRateLimitManager()
        );

        // Initialize reloader with current config
        reloader.setCurrentConfig(config);

        // Start config watcher
        configWatcher = new ConfigWatcher(vertx, configPath, reloader);
        configWatcher.start();

        log.info("=================================================");
        log.info("    All Gateway Servers Started Successfully!");
        log.info("=================================================");
    }

    public void stop() {
        log.info("Stopping Nacos Gateway...");

        if (configWatcher != null) {
            configWatcher.stop();
        }

        if (gatewayServerManager != null) {
            gatewayServerManager.stop();
        }

        if (vertx != null) {
            vertx.close()
                .onSuccess(v -> log.info("Nacos Gateway stopped"))
                .onFailure(t -> log.error("Error stopping gateway: {}", t.getMessage()));
        }
    }
}
