package pans.gateway.route;

import pans.gateway.config.RouteConfig;

import java.util.UUID;

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
    public String toString() {
        return "Route{" +
                "hostPattern='" + hostPattern + '\'' +
                ", backendName='" + backendName + '\'' +
                '}';
    }
}
