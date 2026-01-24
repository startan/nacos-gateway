package nextf.nacos.gateway.loadbalance;

import nextf.nacos.gateway.model.Endpoint;

import java.util.List;
import java.util.Random;

/**
 * Random load balancer
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    public RandomLoadBalancer() {
    }

    @Override
    public Endpoint select(List<Endpoint> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }

        int index = random.nextInt(endpoints.size());
        return endpoints.get(index);
    }

    @Override
    public String toString() {
        return "RandomLoadBalancer";
    }
}
