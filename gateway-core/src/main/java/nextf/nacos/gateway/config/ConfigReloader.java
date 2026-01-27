package nextf.nacos.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.reader.ConfigFileReader;
import nextf.nacos.gateway.proxy.ConnectionManager;
import nextf.nacos.gateway.ratelimit.RateLimitManager;
import nextf.nacos.gateway.registry.GatewayRegistry;

/**
 * Configuration reloader for hot reloading
 * 职责：协调 ConfigFileReader 和 ConfigLoader，完成配置热加载
 */
public class ConfigReloader {

    private static final Logger log = LoggerFactory.getLogger(ConfigReloader.class);

    private final ConfigLoader configLoader;
    private final GatewayRegistry registry;
    private final RateLimitManager rateLimitManager;
    private final ConfigFileReader configFileReader;

    private GatewayConfig currentConfig;

    public ConfigReloader(ConfigLoader configLoader,
                         GatewayRegistry registry,
                         RateLimitManager rateLimitManager,
                         ConfigFileReader configFileReader) {
        this.configLoader = configLoader;
        this.registry = registry;
        this.rateLimitManager = rateLimitManager;
        this.configFileReader = configFileReader;
    }

    public void setCurrentConfig(GatewayConfig config) {
        this.currentConfig = config;
    }

    /**
     * 重新加载配置（无入参，通过 ConfigFileReader 读取）
     */
    public synchronized void reload() {
        try {
            // 1. 通过 ConfigFileReader 读取最新配置内容
            String configContent = configFileReader.readConfig();
            log.info("Configuration content read from: {}", configFileReader.getSourceDescription());

            // 2. 通过 ConfigLoader 解析和验证配置
            GatewayConfig newConfig = configLoader.loadFromString(configContent);
            log.info("New configuration parsed and validated successfully");

            // 3. 检查端口是否变化（需要重启）
            if (hasPortsChanged(newConfig)) {
                log.warn("Port configuration changed - requires restart for changes to take effect");
            }

            // 4. 更新路由
            if (newConfig.getRoutes() != null) {
                registry.updateRoutes(newConfig.getRoutes());
            }

            // 5. 更新后端
            if (newConfig.getBackends() != null) {
                registry.updateBackends(newConfig.getBackends());
            }

            // 6. 更新限流配置
            updateRateLimiters(newConfig);

            // 7. 更新当前配置
            currentConfig = newConfig;

            log.info("Configuration reloaded successfully");

        } catch (Exception e) {
            log.error("Failed to reload configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Configuration reload failed", e);
        }
    }

    /**
     * Check if port configuration has changed
     */
    private boolean hasPortsChanged(GatewayConfig newConfig) {
        if (currentConfig == null || currentConfig.getServer() == null) {
            return false;
        }

        ServerConfig oldServer = currentConfig.getServer();
        ServerConfig newServer = newConfig.getServer();

        if (oldServer.getPorts() == null || newServer.getPorts() == null) {
            return false;
        }

        return !oldServer.getPorts().equals(newServer.getPorts());
    }

    /**
     * Update rate limiters
     * This stays in ConfigReloader as it's not managed by the registry
     */
    private void updateRateLimiters(GatewayConfig newConfig) {
        boolean updated = false;
        // 1. Update server-level rate limit config (hot reload support)
        if (newConfig.getServer() != null && newConfig.getServer().getRateLimit() != null) {
            rateLimitManager.updateServerRateLimitConfig(newConfig.getServer().getRateLimit());
            updated = true;
        }

        // 2. Update backend rate limiters
        if (newConfig.getBackends() != null) {
            for (BackendConfig backendConfig : newConfig.getBackends()) {
                rateLimitManager.updateBackendLimiter(backendConfig.getName(), backendConfig);
            }
            updated = true;
        }

        if (updated) {
            rateLimitManager.clearClientLimiters();
        }
        log.info("Updated rate limiters (server and backend level)");
    }

    public GatewayConfig getCurrentConfig() {
        return currentConfig;
    }
}
