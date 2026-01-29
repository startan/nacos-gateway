package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rate limit configuration
 * Used by both server-level and backend-level rate limiting
 */
public class RateLimitConfig {

    @JsonProperty("maxQps")
    private int maxQps = 2000;

    @JsonProperty("maxConnections")
    private int maxConnections = 10000;

    @JsonProperty("maxQpsPerClient")
    private int maxQpsPerClient = 10;

    @JsonProperty("maxConnectionsPerClient")
    private int maxConnectionsPerClient = 5;

    public int getMaxQps() {
        return maxQps;
    }

    public void setMaxQps(int maxQps) {
        this.maxQps = maxQps;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxQpsPerClient() {
        return maxQpsPerClient;
    }

    public void setMaxQpsPerClient(int maxQpsPerClient) {
        this.maxQpsPerClient = maxQpsPerClient;
    }

    public int getMaxConnectionsPerClient() {
        return maxConnectionsPerClient;
    }

    public void setMaxConnectionsPerClient(int maxConnectionsPerClient) {
        this.maxConnectionsPerClient = maxConnectionsPerClient;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "maxQps=" + maxQps +
                ", maxConnections=" + maxConnections +
                ", maxQpsPerClient=" + maxQpsPerClient +
                ", maxConnectionsPerClient=" + maxConnectionsPerClient +
                '}';
    }
}
