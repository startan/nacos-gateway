package nextf.nacos.gateway.health;

import nextf.nacos.gateway.model.Endpoint;

/**
 * Health checker interface
 */
public interface HealthChecker {

    /**
     * Check if an endpoint is healthy
     */
    boolean isHealthy(Endpoint endpoint);

    /**
     * Set the health status of an endpoint
     */
    void setHealthStatus(Endpoint endpoint, boolean healthy);

    /**
     * Start health checking for an endpoint
     */
    void startChecking(Endpoint endpoint);

    /**
     * Stop health checking for an endpoint
     */
    void stopChecking(Endpoint endpoint);
}
