package pans.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.GatewayConfig;
import pans.gateway.config.PortType;
import pans.gateway.config.event.EntityChangeEvent;
import pans.gateway.config.event.EntityChangeListener;
import pans.gateway.config.event.RoutesUpdatedEvent;
import pans.gateway.health.HealthCheckManager;
import pans.gateway.loadbalance.EndpointSelector;
import pans.gateway.management.HealthEndpoint;
import pans.gateway.model.Backend;
import pans.gateway.proxy.ConnectionManager;
import pans.gateway.ratelimit.RateLimitManager;
import pans.gateway.registry.GatewayRegistry;
import pans.gateway.route.RouteMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple GatewayServer instances, one per port type
 * Shared components (GatewayRegistry, HealthCheckManager, RateLimitManager) are used across all servers
 */
public class GatewayServerManager implements EntityChangeListener {

    private static final Logger log = LoggerFactory.getLogger(GatewayServerManager.class);

    private final Vertx vertx;
    private final GatewayConfig config;
    private final String configPath;

    // One server per port type
    private final Map<PortType, GatewayServer> servers = new ConcurrentHashMap<>();

    // Shared components across all servers
    private GatewayRegistry registry;
    private EndpointSelector endpointSelector;
    private ConnectionManager connectionManager;
    private HealthCheckManager healthCheckManager;
    private RateLimitManager rateLimitManager;
    private HealthEndpoint healthEndpoint;

    public GatewayServerManager(Vertx vertx, GatewayConfig config, String configPath) {
        this.vertx = vertx;
        this.config = config;
        this.configPath = configPath;
    }

    /**
     * Start all gateway servers
     */
    public void start() {
        log.info("=================================================");
        log.info("    Nacos Gateway - Starting...");
        log.info("=================================================");

        // Initialize shared components
        initializeSharedComponents();

        // Start a server for each port type
        for (PortType portType : PortType.values()) {
            int port = config.getServer().getPortForType(portType.getConfigName());

            GatewayServer server = new GatewayServer(
                vertx,
                config,
                configPath,
                portType,
                port,
                registry.getRouteMatcher(),
                registry.getBackends(),
                endpointSelector,
                connectionManager,
                rateLimitManager,
                healthEndpoint
            );

            servers.put(portType, server);
            server.start();

            log.info("Started {} server on port {}", portType.getDescription(), port);
        }

        log.info("=================================================");
        log.info("    Nacos Gateway - Started Successfully!");
        log.info("=================================================");
    }

    /**
     * Stop all gateway servers
     */
    public void stop() {
        log.info("=================================================");
        log.info("    Stopping Nacos Gateway...");
        log.info("=================================================");

        servers.values().forEach(GatewayServer::stop);
        servers.clear();

        if (healthCheckManager != null) {
            healthCheckManager.stopAll();
        }

        if (connectionManager != null) {
            connectionManager.closeAll();
        }

        log.info("=================================================");
        log.info("    Nacos Gateway Stopped!");
        log.info("=================================================");
    }

    /**
     * Initialize shared components used by all gateway servers
     */
    private void initializeSharedComponents() {
        log.info("Initializing shared gateway components...");

        // Initialize rate limit manager first (needed by ConnectionManager)
        rateLimitManager = new RateLimitManager(config);

        // Initialize connection manager (shared across all servers)
        connectionManager = new ConnectionManager(rateLimitManager);

        // Initialize registry - central entity management
        registry = new GatewayRegistry();

        // Initialize routes in registry
        if (config.getRoutes() != null) {
            registry.updateRoutes(config.getRoutes());
        }

        // Initialize backends in registry
        if (config.getBackends() != null) {
            registry.updateBackends(config.getBackends());
        }

        // Register this manager as an entity change listener
        registry.registerListener(this);

        // Initialize health check manager (shared across all servers)
        healthCheckManager = new HealthCheckManager(vertx, registry);

        // Initialize endpoint selector
        endpointSelector = new EndpointSelector();

        // Initialize health endpoint
        var mgmtConfig = config.getManagement();
        if (mgmtConfig != null && mgmtConfig.getHealth() != null && mgmtConfig.getHealth().isEnabled()) {
            healthEndpoint = new HealthEndpoint(mgmtConfig.getHealth().getPath());
            log.info("Health endpoint enabled: {}", healthEndpoint.getPath());
        }

        // Update backend rate limiters
        if (config.getBackends() != null) {
            for (var backendConfig : config.getBackends()) {
                if (backendConfig.hasCustomRateLimit()) {
                    rateLimitManager.updateBackendLimiter(backendConfig.getName(), backendConfig);
                }
            }
        }

        log.info("Shared gateway components initialized");
    }

    /**
     * Handle entity change events
     * Disconnect invalid connections when routes change
     */
    @Override
    public void onEntityChanged(EntityChangeEvent event) {
        if (event instanceof RoutesUpdatedEvent) {
            log.info("Routes changed, checking for invalid connections...");
            connectionManager.disconnectInvalidConnections(registry.getRouteMatcher());
        }
    }

    // Getters for access to shared components
    public RouteMatcher getRouteMatcher() {
        return registry.getRouteMatcher();
    }

    public Map<String, Backend> getBackends() {
        return registry.getBackends();
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public HealthCheckManager getHealthCheckManager() {
        return healthCheckManager;
    }

    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }

    public GatewayRegistry getRegistry() {
        return registry;
    }

    public GatewayServer getServer(PortType portType) {
        return servers.get(portType);
    }
}
