package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Route configuration
 */
public class RouteConfig {

    @JsonProperty("host")
    private String host;

    @JsonProperty("path")
    private String path;

    @JsonProperty("backend")
    private String backend;

    @JsonProperty("qpsLimit")
    private Integer qpsLimit;

    @JsonProperty("maxConnections")
    private Integer maxConnections;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public Integer getQpsLimit() {
        return qpsLimit;
    }

    public void setQpsLimit(Integer qpsLimit) {
        this.qpsLimit = qpsLimit;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    @Override
    public String toString() {
        return "RouteConfig{" +
                "host='" + host + '\'' +
                ", path='" + path + '\'' +
                ", backend='" + backend + '\'' +
                ", qpsLimit=" + qpsLimit +
                ", maxConnections=" + maxConnections +
                '}';
    }
}
