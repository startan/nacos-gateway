package nextf.nacos.gateway.route;

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

    public Route(RouteConfig config) {
        this.hostPattern = config.getHost();
        this.backendName = config.getBackend();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(hostPattern, route.hostPattern) &&
               Objects.equals(backendName, route.backendName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostPattern, backendName);
    }

    @Override
    public String toString() {
        return "Route{" +
                "hostPattern='" + hostPattern + '\'' +
                ", backendName='" + backendName + '\'' +
                '}';
    }
}
