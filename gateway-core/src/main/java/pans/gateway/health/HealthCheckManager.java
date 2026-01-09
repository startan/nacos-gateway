package pans.gateway.health;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.BackendConfig;
import pans.gateway.config.HealthProbeConfig;
import pans.gateway.model.Endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Health check manager
 */
public class HealthCheckManager {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckManager.class);

    private final Vertx vertx;
    private final Map<Endpoint, HealthCheckTask> tasks = new ConcurrentHashMap<>();

    public HealthCheckManager(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Start health checking for all endpoints in a backend
     */
    public void startBackendChecking(BackendConfig backendConfig, List<Endpoint> endpoints) {
        HealthProbeConfig probeConfig = backendConfig.getProbe();
        if (probeConfig == null) {
            log.info("No health probe configured for backend: {}", backendConfig.getName());
            // Mark all endpoints as healthy
            for (Endpoint endpoint : endpoints) {
                endpoint.setHealthy(true);
            }
            return;
        }

        for (Endpoint endpoint : endpoints) {
            startChecking(endpoint, probeConfig);
        }
    }

    /**
     * Start health checking for an endpoint
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
        for (Map.Entry<Endpoint, HealthCheckTask> entry : tasks.entrySet()) {
            entry.getValue().stop();
        }
        tasks.clear();
    }

    /**
     * Check if an endpoint is healthy
     */
    public boolean isHealthy(Endpoint endpoint) {
        HealthCheckTask task = tasks.get(endpoint);
        if (task == null) {
            // No health check configured, assume healthy
            return endpoint.isHealthy();
        }
        return task.isHealthy();
    }

    /**
     * Get number of active health check tasks
     */
    public int getActiveCheckCount() {
        return tasks.size();
    }
}
