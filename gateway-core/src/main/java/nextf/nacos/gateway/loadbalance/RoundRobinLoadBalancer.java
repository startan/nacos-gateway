package nextf.nacos.gateway.loadbalance;

import nextf.nacos.gateway.model.Endpoint;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin load balancer
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public RoundRobinLoadBalancer() {
    }

    @Override
    public Endpoint select(List<Endpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % endpoints.size());
        return endpoints.get(index);
    }

    @Override
    public String toString() {
        return "RoundRobinLoadBalancer";
    }
}
