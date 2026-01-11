package pans.gateway.route;

import pans.gateway.config.RouteConfig;

import java.util.UUID;

/**
 * Route entity
 */
public class Route {

    private final String hostPattern;
    private final String pathPattern;
    private final String backendName;

    public Route(RouteConfig config) {
        this.hostPattern = config.getHost();
        this.pathPattern = config.getPath();
        this.backendName = config.getBackend();
    }

    public String getId() {
        return hostPattern + '#' + pathPattern;
    }

    public String getHostPattern() {
        return hostPattern;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public String getBackendName() {
        return backendName;
    }

    @Override
    public String toString() {
        return "Route{" +
                "hostPattern='" + hostPattern + '\'' +
                ", pathPattern='" + pathPattern + '\'' +
                ", backendName='" + backendName + '\'' +
                '}';
    }
}
