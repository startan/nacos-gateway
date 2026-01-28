package nextf.nacos.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.ConfigLoader;
import nextf.nacos.gateway.config.ConfigReloader;
import nextf.nacos.gateway.config.ConfigWatcher;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.reader.ConfigFileReader;
import nextf.nacos.gateway.config.reader.ConfigFileReaderFactory;

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
        // 1. Create Vert.x instance (FileConfigReader needs Vertx to set up timers)
        vertx = Vertx.vertx();

        // 2. Create ConfigFileReader
        ConfigFileReader configFileReader = ConfigFileReaderFactory.getReader(configPath, vertx);

        // 3. Read configuration content
        String configContent = configFileReader.readConfig();

        // 4. Parse configuration
        ConfigLoader configLoader = new ConfigLoader();
        GatewayConfig config = configLoader.loadFromString(configContent);

        // 5. Create and start gateway server manager
        gatewayServerManager = new GatewayServerManager(vertx, config);
        gatewayServerManager.start();

        // 6. Create ConfigReloader (pass ConfigFileReader)
        ConfigReloader reloader = new ConfigReloader(
                configLoader,
                gatewayServerManager.getRegistry(),
                gatewayServerManager.getRateLimitManager(),
                configFileReader  // new parameter
        );

        // 7. Initialize reloader and set current configuration
        reloader.setCurrentConfig(config);

        // 8. Create and start ConfigWatcher (pass ConfigFileReader and ConfigReloader)
        configWatcher = new ConfigWatcher(configFileReader, reloader);
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
