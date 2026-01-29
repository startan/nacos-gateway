package nextf.nacos.gateway.model;

import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.config.PortType;
import nextf.nacos.gateway.config.RateLimitConfig;
import nextf.nacos.gateway.loadbalance.LoadBalancer;
import nextf.nacos.gateway.loadbalance.LoadBalancerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Backend service model
 */
public class Backend {

    private final String name;
    private final LoadBalancer loadBalancer;
    private final List<Endpoint> endpoints;
    private final BackendConfig backendConfig;

    /**
     * Static factory method to build a Backend from configuration
     * @param config Backend configuration
     * @return New Backend instance
     */
    public static Backend from(BackendConfig config) {
        // Create endpoints
        List<Endpoint> endpoints = new ArrayList<>();
        if (config.getEndpoints() != null) {
            for (var endpointConfig : config.getEndpoints()) {
                Endpoint endpoint = Endpoint.from(endpointConfig, config.getPorts());
                endpoints.add(endpoint);
            }
        }

        // Create load balancer
        LoadBalancer loadBalancer = LoadBalancerFactory.create(config.getLoadBalance());

        // Create and return backend
        return new Backend(config.getName(), loadBalancer, endpoints, config);
    }

    /**
     * Static factory method to build a map of backends from backend configurations
     * @param backendConfigs List of backend configurations
     * @return Map of backend name to Backend
     */
    public static Map<String, Backend> fromList(List<BackendConfig> backendConfigs) {
        Map<String, Backend> backendsMap = new ConcurrentHashMap<>();
        if (backendConfigs != null) {
            for (BackendConfig config : backendConfigs) {
                Backend backend = Backend.from(config);
                backendsMap.put(backend.getName(), backend);
            }
        }
        return backendsMap;
    }

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
    public RateLimitConfig getRateLimitConfig() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Backend backend = (Backend) o;
        return Objects.equals(name, backend.name) &&
               Objects.equals(endpoints, backend.endpoints) &&
               equalsConfig(backendConfig, backend.backendConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, endpoints);
    }

    /**
     * Compare backend configurations for equality
     */
    private boolean equalsConfig(BackendConfig c1, BackendConfig c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;

        BackendConfig.BackendPortsConfig ports1 = c1.getPorts();
        BackendConfig.BackendPortsConfig ports2 = c2.getPorts();

        return Objects.equals(ports1, ports2);
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
