package pans.gateway.model;

import pans.gateway.config.BackendConfig;
import pans.gateway.config.BackendConfig.BackendRateLimitConfig;
import pans.gateway.config.PortType;
import pans.gateway.loadbalance.LoadBalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Backend service model
 */
public class Backend {

    private final String name;
    private final LoadBalancer loadBalancer;
    private final List<Endpoint> endpoints;
    private final BackendConfig backendConfig;

    public Backend(String name, LoadBalancer loadBalancer, List<Endpoint> endpoints, BackendConfig backendConfig) {
        this.name = name;
        this.loadBalancer = loadBalancer;
        this.endpoints = new ArrayList<>(endpoints);
        this.backendConfig = backendConfig;
    }

    /**
     * Legacy constructor for backward compatibility
     */
    public Backend(String name, LoadBalancer loadBalancer, List<Endpoint> endpoints) {
        this(name, loadBalancer, endpoints, null);
    }

    public String getName() {
        return name;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public List<Endpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    public List<Endpoint> getHealthyEndpoints() {
        return endpoints.stream()
                .filter(Endpoint::isHealthy)
                .collect(Collectors.toList());
    }

    /**
     * Get backend configuration
     * @return the backend configuration, or null if not available
     */
    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

    /**
     * Get rate limit configuration for this backend
     * @return the rate limit configuration, or null if not configured
     */
    public BackendRateLimitConfig getRateLimitConfig() {
        return backendConfig != null ? backendConfig.getRateLimit() : null;
    }

    /**
     * Get port for specific port type
     * @param portType the port type
     * @return the port number
     */
    public int getPortForType(PortType portType) {
        if (backendConfig == null || backendConfig.getPorts() == null) {
            throw new IllegalStateException("Backend '" + name + "' does not have port configuration");
        }
        return backendConfig.getPorts().getPortForType(portType.getConfigName());
    }

    /**
     * Get port for specific port type by config name
     * @param portType the port type config name
     * @return the port number
     */
    public int getPortForType(String portType) {
        return getPortForType(PortType.fromConfigName(portType));
    }

    @Override
    public String toString() {
        return "Backend{" +
                "name='" + name + '\'' +
                ", loadBalancer=" + loadBalancer.getClass().getSimpleName() +
                ", endpoints=" + endpoints +
                '}';
    }
}
