package nextf.nacos.gateway.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.model.Endpoint;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint selector with priority grouping
 */
public class EndpointSelector {

    private static final Logger log = LoggerFactory.getLogger(EndpointSelector.class);

    /**
     * Select an endpoint from the backend
     * Uses priority grouping: selects from highest priority group that has healthy endpoints
     */
    public Endpoint select(Backend backend) {
        if (backend == null) {
            return null;
        }

        List<Endpoint> healthyEndpoints = backend.getHealthyEndpoints();
        if (healthyEndpoints.isEmpty()) {
            log.warn("No healthy endpoints for backend: {}", backend.getName());
            return null;
        }

        // Group by priority
        Map<Integer, List<Endpoint>> groupedByPriority = healthyEndpoints.stream()
                .collect(Collectors.groupingBy(Endpoint::getPriority));

        // Find highest priority (lowest number = highest priority)
        int highestPriority = groupedByPriority.keySet().stream()
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);

        List<Endpoint> highestPriorityEndpoints = groupedByPriority.get(highestPriority);
        if (highestPriorityEndpoints == null || highestPriorityEndpoints.isEmpty()) {
            log.warn("No endpoints found for highest priority: {}", highestPriority);
            return null;
        }

        log.debug("Selecting from {} endpoints with priority {}", highestPriorityEndpoints.size(), highestPriority);

        // Use load balancer to select from the highest priority group
        LoadBalancer loadBalancer = backend.getLoadBalancer();
        Endpoint selected = loadBalancer.select(highestPriorityEndpoints);

        if (selected != null) {
            log.debug("Selected endpoint: {}", selected.getAddress());
        }

        return selected;
    }
}
