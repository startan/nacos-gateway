package pans.gateway.model;

import pans.gateway.config.BackendConfig;
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

    public Backend(String name, LoadBalancer loadBalancer, List<Endpoint> endpoints) {
        this.name = name;
        this.loadBalancer = loadBalancer;
        this.endpoints = new ArrayList<>(endpoints);
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

    @Override
    public String toString() {
        return "Backend{" +
                "name='" + name + '\'' +
                ", loadBalancer=" + loadBalancer.getClass().getSimpleName() +
                ", endpoints=" + endpoints +
                '}';
    }
}
