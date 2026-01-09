package pans.gateway.loadbalance;

import pans.gateway.model.Endpoint;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Least connection load balancer
 */
public class LeastConnectionLoadBalancer implements LoadBalancer {

    private final Map<Endpoint, AtomicInteger> connections = new ConcurrentHashMap<>();

    public LeastConnectionLoadBalancer() {
    }

    @Override
    public Endpoint select(List<Endpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }

        return endpoints.stream()
                .min(Comparator.comparingInt(e -> connections.getOrDefault(e, new AtomicInteger(0)).get()))
                .orElse(null);
    }

    @Override
    public void onConnectionOpen(Endpoint endpoint) {
        connections.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
    }

    @Override
    public void onConnectionClose(Endpoint endpoint) {
        connections.computeIfPresent(endpoint, (k, v) -> {
            int newValue = v.decrementAndGet();
            if (newValue <= 0) {
                // Remove entry when no connections
                return null;
            }
            return v;
        });
    }

    @Override
    public String toString() {
        return "LeastConnectionLoadBalancer";
    }
}
