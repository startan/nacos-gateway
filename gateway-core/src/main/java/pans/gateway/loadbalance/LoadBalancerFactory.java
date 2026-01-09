package pans.gateway.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating load balancers
 */
public class LoadBalancerFactory {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerFactory.class);

    public static LoadBalancer create(String strategy) {
        if (strategy == null) {
            log.debug("Load balancer strategy not specified, using round-robin");
            return new RoundRobinLoadBalancer();
        }

        switch (strategy.toLowerCase()) {
            case "round-robin":
                return new RoundRobinLoadBalancer();
            case "random":
                return new RandomLoadBalancer();
            case "least-connection":
                return new LeastConnectionLoadBalancer();
            default:
                log.warn("Unknown load balancer strategy: {}, using round-robin", strategy);
                return new RoundRobinLoadBalancer();
        }
    }
}
