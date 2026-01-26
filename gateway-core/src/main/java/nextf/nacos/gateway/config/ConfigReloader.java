package nextf.nacos.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.proxy.ConnectionManager;
import nextf.nacos.gateway.ratelimit.RateLimitManager;
import nextf.nacos.gateway.registry.GatewayRegistry;

/**
 * Configuration reloader for hot reloading
 * Now acts as a notifier only - delegates all building logic to GatewayRegistry
 */
public class ConfigReloader {

    private static final Logger log = LoggerFactory.getLogger(ConfigReloader.class);

    private final ConfigLoader configLoader;
    private final GatewayRegistry registry;
    private final RateLimitManager rateLimitManager;

    private GatewayConfig currentConfig;

    public ConfigReloader(ConfigLoader configLoader,
                         GatewayRegistry registry,
                         RateLimitManager rateLimitManager) {
        this.configLoader = configLoader;
        this.registry = registry;
        this.rateLimitManager = rateLimitManager;
    }

    public void setCurrentConfig(GatewayConfig config) {
        this.currentConfig = config;
    }

    /**
     * Reload configuration from file
     * Delegates to GatewayRegistry for atomic update
     */
    public void reload(String configPath) {
        try {
            // Load new configuration
            GatewayConfig newConfig = configLoader.load(configPath);
            log.info("New configuration loaded successfully");

            // Check if ports changed - requires restart
            if (hasPortsChanged(newConfig)) {
                log.warn("Port configuration changed - requires restart for changes to take effect");
            }

            // Update routes via registry
            if (newConfig.getRoutes() != null) {
                registry.updateRoutes(newConfig.getRoutes());
            }

            // Update backends via registry
            if (newConfig.getBackends() != null) {
                registry.updateBackends(newConfig.getBackends());
            }

            // Update rate limiters (this part stays as it's not in registry)
            updateRateLimiters(newConfig);

            // Note: Connection invalidation is handled by GatewayServerManager
            // through RoutesUpdatedEvent and BackendsUpdatedEvent

            // Update current config
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
