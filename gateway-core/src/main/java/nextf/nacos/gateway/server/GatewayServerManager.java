package nextf.nacos.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.PortType;
import nextf.nacos.gateway.config.event.BackendsUpdatedEvent;
import nextf.nacos.gateway.config.event.EntityChangeEvent;
import nextf.nacos.gateway.config.event.EntityChangeListener;
import nextf.nacos.gateway.config.event.RoutesUpdatedEvent;
import nextf.nacos.gateway.health.HealthCheckManager;
import nextf.nacos.gateway.loadbalance.EndpointSelector;
import nextf.nacos.gateway.management.HealthEndpoint;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.proxy.ConnectionManager;
import nextf.nacos.gateway.ratelimit.RateLimitManager;
import nextf.nacos.gateway.registry.GatewayRegistry;
import nextf.nacos.gateway.route.RouteMatcher;
import nextf.nacos.gateway.logging.AccessLogger;

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
    // One server per port type
    private final Map<PortType, GatewayServer> servers = new ConcurrentHashMap<>();

    // Shared components across all servers
    private GatewayRegistry registry;
    private EndpointSelector endpointSelector;
    private ConnectionManager connectionManager;
    private HealthCheckManager healthCheckManager;
    private RateLimitManager rateLimitManager;
    private HealthEndpoint healthEndpoint;
    private AccessLogger accessLogger;

    public GatewayServerManager(Vertx vertx, GatewayConfig config) {
        this.vertx = vertx;
        this.config = config;
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
                portType,
                port,
                registry,
                endpointSelector,
                connectionManager,
                rateLimitManager,
                healthEndpoint,
                accessLogger
            );

            servers.put(portType, server);
            server.start(s -> healthCheckManager.startBackendChecking());

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

        if (accessLogger != null) {
            accessLogger.stop();
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

        // Initialize access logger
        if (config.getAccessLog() != null) {
            accessLogger = new AccessLogger(config.getAccessLog());
            log.info("Access logger {}",
                    accessLogger.isEnabled() ? "enabled" : "disabled");
        } else {
            accessLogger = new AccessLogger(new nextf.nacos.gateway.config.AccessLogConfig());
        }

        // Update backend rate limiters
        if (config.getBackends() != null) {
            for (var backendConfig : config.getBackends()) {
                rateLimitManager.updateBackendLimiter(backendConfig.getName(), backendConfig);
            }
        }

        // Update route rate limiters
        if (config.getRoutes() != null) {
            for (var routeConfig : config.getRoutes()) {
                String routeId = routeConfig.getHost(); // Route.getId() returns hostPattern
                rateLimitManager.updateRouteLimiter(routeId, routeConfig);
            }
        }

        log.info("Shared gateway components initialized");
    }

    /**
     * Handle entity change events
     * Disconnect invalid connections when routes or backends change
     */
    @Override
    public void onEntityChanged(EntityChangeEvent event) {
        if (event instanceof RoutesUpdatedEvent || event instanceof BackendsUpdatedEvent) {
            log.info("Configuration changed, checking for invalid connections...");

            // Only need new configuration from registry
            connectionManager.disconnectInvalidConnections(
                    registry.getRoutes(),
                    registry.getBackends()
            );
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

    public AccessLogger getAccessLogger() {
        return accessLogger;
    }
}
