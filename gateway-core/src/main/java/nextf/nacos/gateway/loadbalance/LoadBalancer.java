package nextf.nacos.gateway.loadbalance;

import nextf.nacos.gateway.model.Endpoint;

import java.util.List;

/**
 * Load balancer interface
 */
public interface LoadBalancer {

    /**
     * Select an endpoint from the list
     */
    Endpoint select(List<Endpoint> endpoints);

    /**
     * Called when a connection is opened to an endpoint
     */
    default void onConnectionOpen(Endpoint endpoint) {
        // Default: no-op
    }

    /**
     * Called when a connection is closed to an endpoint
     */
    default void onConnectionClose(Endpoint endpoint) {
        // Default: no-op
    }
}
