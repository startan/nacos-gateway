package pans.gateway.server;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.BackendConfig;
import pans.gateway.config.GatewayConfig;
import pans.gateway.config.PortType;
import pans.gateway.config.RouteConfig;
import pans.gateway.health.HealthCheckManager;
import pans.gateway.loadbalance.EndpointSelector;
import pans.gateway.loadbalance.LoadBalancerFactory;
import pans.gateway.management.HealthEndpoint;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.proxy.ConnectionManager;
import pans.gateway.ratelimit.RateLimitManager;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple GatewayServer instances, one per port type
 * Shared components (RouteMatcher, Backends, HealthCheckManager, RateLimitManager) are used across all servers
 */
public class GatewayServerManager {

    private static final Logger log = LoggerFactory.getLogger(GatewayServerManager.class);

    private final Vertx vertx;
    private final GatewayConfig config;
    private final String configPath;

    // One server per port type
    private final Map<PortType, GatewayServer> servers = new ConcurrentHashMap<>();

    // Shared components across all servers
    private RouteMatcher routeMatcher;
    private Map<String, Backend> backends;
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
                routeMatcher,
                backends,
                endpointSelector,
                connectionManager,
                healthCheckManager,
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

        // Initialize health check manager (shared across all servers)
        healthCheckManager = new HealthCheckManager(vertx);

        // Initialize routes and backends
        initializeRoutesAndBackends();

        // Initialize health endpoint
        var mgmtConfig = config.getManagement();
        if (mgmtConfig != null && mgmtConfig.getHealth() != null && mgmtConfig.getHealth().isEnabled()) {
            healthEndpoint = new HealthEndpoint(mgmtConfig.getHealth().getPath());
            log.info("Health endpoint enabled: {}", healthEndpoint.getPath());
        }

        log.info("Shared gateway components initialized");
    }

    /**
     * Initialize routes and backends from configuration
     */
    private void initializeRoutesAndBackends() {
        // Create routes
        List<Route> routes = new ArrayList<>();
        if (config.getRoutes() != null) {
            for (RouteConfig routeConfig : config.getRoutes()) {
                Route route = new Route(routeConfig);
                routes.add(route);
            }
        }

        routeMatcher = new pans.gateway.route.RouteMatcherImpl(routes);
        log.info("Loaded {} routes", routes.size());

        // Create backends
        backends = new ConcurrentHashMap<>();
        if (config.getBackends() != null) {
            for (BackendConfig backendConfig : config.getBackends()) {
                // Create endpoints with port configuration
                List<Endpoint> endpoints = new ArrayList<>();
                if (backendConfig.getEndpoints() != null) {
                    for (var endpointConfig : backendConfig.getEndpoints()) {
                        Endpoint endpoint = new Endpoint(endpointConfig, backendConfig.getPorts());
                        endpoints.add(endpoint);
                    }
                }

                // Create load balancer
                var loadBalancer = LoadBalancerFactory.create(backendConfig.getLoadBalance());

                // Create backend with config reference
                Backend backend = new Backend(backendConfig.getName(), loadBalancer, endpoints, backendConfig);
                backends.put(backend.getName(), backend);

                // Start health checking (using apiV1 port)
                healthCheckManager.startBackendChecking(backendConfig, endpoints);

                // Update backend rate limiter if configured
                if (backendConfig.hasCustomRateLimit()) {
                    rateLimitManager.updateBackendLimiter(backendConfig.getName(), backendConfig);
                }
            }
        }

        endpointSelector = new pans.gateway.loadbalance.EndpointSelector();
        log.info("Loaded {} backends", backends.size());
    }

    // Getters for access to shared components
    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }

    public Map<String, Backend> getBackends() {
        return backends;
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

    public GatewayServer getServer(PortType portType) {
        return servers.get(portType);
    }
}
