package pans.gateway.route;

import pans.gateway.config.RouteConfig;

import java.util.UUID;

/**
 * Route entity
 */
public class Route {

    private final String id;
    private final String hostPattern;
    private final String pathPattern;
    private final String backendName;
    private final Integer qpsLimit;
    private final Integer maxConnections;

    public Route(RouteConfig config) {
        this.id = UUID.randomUUID().toString();
        this.hostPattern = config.getHost();
        this.pathPattern = config.getPath();
        this.backendName = config.getBackend();
        this.qpsLimit = config.getQpsLimit();
        this.maxConnections = config.getMaxConnections();
    }

    public String getId() {
        return id;
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

    public Integer getQpsLimit() {
        return qpsLimit;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", hostPattern='" + hostPattern + '\'' +
                ", pathPattern='" + pathPattern + '\'' +
                ", backendName='" + backendName + '\'' +
                ", qpsLimit=" + qpsLimit +
                ", maxConnections=" + maxConnections +
                '}';
    }
}
