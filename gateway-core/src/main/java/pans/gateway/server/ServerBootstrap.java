package pans.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.ConfigLoader;
import pans.gateway.config.ConfigReloader;
import pans.gateway.config.ConfigWatcher;
import pans.gateway.config.GatewayConfig;

/**
 * Server bootstrap
 */
public class ServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    private final String configPath;
    private Vertx vertx;
    private GatewayServer gatewayServer;
    private ConfigWatcher configWatcher;

    public ServerBootstrap(String configPath) {
        this.configPath = configPath;
    }

    public void start() throws Exception {
        log.info("=================================================");
        log.info("    Nacos Gateway - Starting...");
        log.info("=================================================");

        // Load configuration
        ConfigLoader configLoader = new ConfigLoader();
        GatewayConfig config = configLoader.load(configPath);

        // Create Vert.x instance
        vertx = Vertx.vertx();

        // Create and start gateway server
        gatewayServer = new GatewayServer(vertx, config, configPath);
        gatewayServer.start();

        // Create config reloader
        ConfigReloader reloader = new ConfigReloader(
                configLoader,
                gatewayServer.getConnectionManager(),
                gatewayServer.getHealthCheckManager(),
                gatewayServer.getRateLimitManager()
        );

        // Initialize reloader with current state
        reloader.setRouteMatcher(gatewayServer.getRouteMatcher());
        reloader.setBackends(gatewayServer.getBackends());

        // Start config watcher
        configWatcher = new ConfigWatcher(vertx, configPath, reloader);
        configWatcher.start();

        log.info("=================================================");
        log.info("    Nacos Gateway - Started Successfully!");
        log.info("    Listening on port: {}", config.getServer().getPort());
        log.info("=================================================");
    }

    public void stop() {
        log.info("Stopping Nacos Gateway...");

        if (configWatcher != null) {
            configWatcher.stop();
        }

        if (gatewayServer != null) {
            gatewayServer.stop();
        }

        if (vertx != null) {
            vertx.close()
                .onSuccess(v -> log.info("Nacos Gateway stopped"))
                .onFailure(t -> log.error("Error stopping gateway: {}", t.getMessage()));
        }
    }
}
