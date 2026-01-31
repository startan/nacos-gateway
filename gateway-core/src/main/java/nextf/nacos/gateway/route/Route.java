package nextf.nacos.gateway.route;

import nextf.nacos.gateway.config.RateLimitConfig;
import nextf.nacos.gateway.config.RouteConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Route entity
 */
public class Route {

    private final String hostPattern;
    private final String backendName;
    private final RateLimitConfig rateLimitConfig;

    public Route(RouteConfig config) {
        this.hostPattern = config.getHost();
        this.backendName = config.getBackend();
        this.rateLimitConfig = config.getRateLimit();
    }

    /**
     * Static factory method to build a map of routes from route configurations
     * @param routeConfigs List of route configurations
     * @return Map of route ID to Route
     */
    public static Map<String, Route> from(List<RouteConfig> routeConfigs) {
        Map<String, Route> routesMap = new ConcurrentHashMap<>();
        if (routeConfigs != null) {
            for (RouteConfig config : routeConfigs) {
                Route route = new Route(config);
                routesMap.put(route.getId(), route);
            }
        }
        return routesMap;
    }

    public String getId() {
        return hostPattern;
    }

    public String getHostPattern() {
        return hostPattern;
    }

    public String getBackendName() {
        return backendName;
    }

    public RateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(hostPattern, route.hostPattern) &&
               Objects.equals(backendName, route.backendName) &&
               Objects.equals(rateLimitConfig, route.rateLimitConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostPattern, backendName, rateLimitConfig);
    }

    @Override
    public String toString() {
        return "Route{" +
                "hostPattern='" + hostPattern + '\'' +
                ", backendName='" + backendName + '\'' +
                ", rateLimitConfig=" + rateLimitConfig +
                '}';
    }
}
