package pans.gateway.health;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.BackendConfig;
import pans.gateway.config.HealthProbeConfig;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.registry.GatewayRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Health check manager
 * Manages health check tasks for all endpoints
 * Updates health status directly on cached Endpoint objects (no events needed)
 * Health checks always use apiV1 port for backend services
 */
public class HealthCheckManager {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckManager.class);

    private final Vertx vertx;
    private final GatewayRegistry registry;
    private final Map<Endpoint, HealthCheckTask> tasks = new ConcurrentHashMap<>();

    public HealthCheckManager(Vertx vertx, GatewayRegistry registry) {
        this.vertx = vertx;
        this.registry = registry;
    }

    /**
     * Start health checking for all backends
     * Gets endpoints from registry and updates them directly (no temporary objects)
     */
    public void startBackendChecking() {
        Map<String, Backend> backends = registry.getBackends();

        for (Backend backend : backends.values()) {
            BackendConfig backendConfig = backend.getBackendConfig();
            if (backendConfig == null) {
                // unreachable branch
                continue;
            }
            HealthProbeConfig probeConfig = backendConfig.getProbe();
            // Check if probe is configured and enabled
            if (probeConfig == null || !probeConfig.isEnabled()) {
                log.info("Health check disabled for backend: {}", backendConfig.getName());
                // Mark all endpoints as healthy
                for (Endpoint endpoint : backend.getEndpoints()) {
                    endpoint.setHealthy(true);
                }
                continue;
            }

            // Start health checks for each endpoint
            // Note: Endpoint objects should be from registry
            for (Endpoint endpoint : backend.getEndpoints()) {
                startChecking(endpoint, probeConfig);
                log.info("Started health check for {} using apiV1 port {}",
                        endpoint.getHost(), endpoint.getApiV1Port());
            }
        }
    }

    /**
     * Start health checking for an endpoint
     * Updates the endpoint's health status directly in the cache
     */
    public void startChecking(Endpoint endpoint, HealthProbeConfig config) {
        HealthCheckTask existingTask = tasks.get(endpoint);
        if (existingTask != null) {
            log.debug("Health check already running for endpoint: {}", endpoint.getAddress());
            return;
        }

        HealthCheckTask task = new HealthCheckTask(vertx, endpoint, config);
        tasks.put(endpoint, task);
        task.start();
        log.info("Started health check for endpoint: {}", endpoint.getAddress());
    }

    /**
     * Stop health checking for an endpoint
     */
    public void stopChecking(Endpoint endpoint) {
        HealthCheckTask task = tasks.remove(endpoint);
        if (task != null) {
            task.stop();
            log.info("Stopped health check for endpoint: {}", endpoint.getAddress());
        }
    }

    /**
     * Stop all health checks
     */
    public void stopAll() {
        log.info("Stopping all health checks");
        HashSet<Endpoint> removeSet = new HashSet<>(tasks.keySet());
        for (Endpoint endpoint : removeSet) {
            stopChecking(endpoint);
        }
    }

    /**
     * Get number of active health check tasks
     */
    public int getActiveCheckCount() {
        return tasks.size();
    }
}
