package pans.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.health.HealthCheckManager;
import pans.gateway.loadbalance.EndpointSelector;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.proxy.ConnectionManager;
import pans.gateway.ratelimit.RateLimitManager;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;
import pans.gateway.route.RouteMatcherImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration reloader for hot reloading
 */
public class ConfigReloader {

    private static final Logger log = LoggerFactory.getLogger(ConfigReloader.class);

    private final ConfigLoader configLoader;
    private final ConnectionManager connectionManager;
    private final HealthCheckManager healthCheckManager;
    private final RateLimitManager rateLimitManager;

    private RouteMatcher routeMatcher;
    private Map<String, Backend> backends;
    private EndpointSelector endpointSelector;

    public ConfigReloader(ConfigLoader configLoader,
                         ConnectionManager connectionManager,
                         HealthCheckManager healthCheckManager,
                         RateLimitManager rateLimitManager) {
        this.configLoader = configLoader;
        this.connectionManager = connectionManager;
        this.healthCheckManager = healthCheckManager;
        this.rateLimitManager = rateLimitManager;
    }

    public void setRouteMatcher(RouteMatcher routeMatcher) {
        this.routeMatcher = routeMatcher;
    }

    public void setBackends(Map<String, Backend> backends) {
        this.backends = backends;
    }

    public void setEndpointSelector(EndpointSelector endpointSelector) {
        this.endpointSelector = endpointSelector;
    }

    /**
     * Reload configuration from file
     */
    public void reload(String configPath) {
        try {
            // Load new configuration
            GatewayConfig newConfig = configLoader.load(configPath);
            log.info("New configuration loaded successfully");

            // Update routes
            updateRoutes(newConfig);

            // Update backends and health checks
            updateBackends(newConfig);

            // Update rate limiters
            updateRateLimiters(newConfig);

            // Disconnect invalid connections
            if (routeMatcher != null) {
                connectionManager.disconnectInvalidConnections(routeMatcher);
            }

            log.info("Configuration reloaded successfully");

        } catch (Exception e) {
            log.error("Failed to reload configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Configuration reload failed", e);
        }
    }

    private void updateRoutes(GatewayConfig newConfig) {
        List<Route> newRoutes = new ArrayList<>();
        if (newConfig.getRoutes() != null) {
            for (RouteConfig routeConfig : newConfig.getRoutes()) {
                Route route = new Route(routeConfig);
                newRoutes.add(route);
            }
        }

        if (routeMatcher == null) {
            routeMatcher = new RouteMatcherImpl(newRoutes);
        } else {
            ((RouteMatcherImpl) routeMatcher).updateRoutes(newRoutes);
        }

        log.info("Updated {} routes", newRoutes.size());
    }

    private void updateBackends(GatewayConfig newConfig) {
        Map<String, Backend> newBackends = new HashMap<>();

        if (newConfig.getBackends() != null) {
            for (BackendConfig backendConfig : newConfig.getBackends()) {
                // Create endpoints
                List<Endpoint> endpoints = new ArrayList<>();
                for (var endpointConfig : backendConfig.getEndpoints()) {
                    Endpoint endpoint = new Endpoint(endpointConfig);
                    endpoints.add(endpoint);
                }

                // Create load balancer
                var loadBalancer = pans.gateway.loadbalance.LoadBalancerFactory.create(
                        backendConfig.getLoadBalance()
                );

                // Create backend
                Backend backend = new Backend(
                        backendConfig.getName(),
                        loadBalancer,
                        endpoints
                );

                newBackends.put(backend.getName(), backend);

                // Start health checking
                healthCheckManager.startBackendChecking(backendConfig, endpoints);
            }
        }

        // Update endpoint selector
        if (endpointSelector == null) {
            endpointSelector = new EndpointSelector();
        }

        backends = newBackends;
        log.info("Updated {} backends", newBackends.size());
    }

    private void updateRateLimiters(GatewayConfig newConfig) {
        // Update route-level rate limiters
        if (newConfig.getRoutes() != null && routeMatcher != null) {
            for (RouteConfig routeConfig : newConfig.getRoutes()) {
                // Find the corresponding route
                // Note: This is a simplified approach, in practice you'd want to match by ID
                rateLimitManager.updateRouteLimiter(
                        routeConfig.getHost() + ":" + routeConfig.getPath(),
                        routeConfig,
                        newConfig
                );
            }
        }

        log.info("Updated rate limiters");
    }

    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }

    public Map<String, Backend> getBackends() {
        return backends;
    }

    public EndpointSelector getEndpointSelector() {
        return endpointSelector;
    }
}
