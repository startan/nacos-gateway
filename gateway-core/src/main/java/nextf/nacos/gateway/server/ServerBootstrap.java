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
        // 1. 创建 Vert.x 实例（FileConfigReader 需要 Vertx 来设置定时器）
        vertx = Vertx.vertx();

        // 2. 创建 ConfigFileReader
        ConfigFileReader configFileReader = ConfigFileReaderFactory.getReader(configPath, vertx);

        // 3. 读取配置内容
        String configContent = configFileReader.readConfig();

        // 4. 解析配置
        ConfigLoader configLoader = new ConfigLoader();
        GatewayConfig config = configLoader.loadFromString(configContent);

        // 5. 创建并启动网关服务器管理器
        gatewayServerManager = new GatewayServerManager(vertx, config);
        gatewayServerManager.start();

        // 6. 创建 ConfigReloader（传入 ConfigFileReader）
        ConfigReloader reloader = new ConfigReloader(
                configLoader,
                gatewayServerManager.getRegistry(),
                gatewayServerManager.getRateLimitManager(),
                configFileReader  // 新增参数
        );

        // 7. 初始化 reloader 并设置当前配置
        reloader.setCurrentConfig(config);

        // 8. 创建并启动 ConfigWatcher（传入 ConfigFileReader 和 ConfigReloader）
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
