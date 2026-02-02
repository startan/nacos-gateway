package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rate limit configuration
 * Used by both server-level and backend-level rate limiting
 *
 * Value semantics:
 * - -1: no limit (unlimited)
 * - 0: reject all access (most extreme restriction)
 * - > 0: normal limit
 */
public class RateLimitConfig {

    @JsonProperty("maxQps")
    private int maxQps = -1;

    @JsonProperty("maxConnections")
    private int maxConnections = -1;

    @JsonProperty("maxQpsPerClient")
    private int maxQpsPerClient = -1;

    @JsonProperty("maxConnectionsPerClient")
    private int maxConnectionsPerClient = -1;

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

    // Helper methods
    // Note: 0 means "reject all", -1 means "no limit", >0 means "limited"
    public boolean isQpsLimited() { return maxQps != -1; }
    public boolean isConnectionsLimited() { return maxConnections != -1; }
    public boolean isQpsPerClientLimited() { return maxQpsPerClient != -1; }
    public boolean isConnectionsPerClientLimited() { return maxConnectionsPerClient != -1; }
    public boolean isQpsRejected() { return maxQps == 0; }
    public boolean isConnectionsRejected() { return maxConnections == 0; }

    /**
     * Check if this config is exactly the default (all -1, unlimited)
     * @return true if all fields are -1
     */
    public boolean isDefaultUnlimited() {
        return maxQps == -1 && maxConnections == -1 &&
               maxQpsPerClient == -1 && maxConnectionsPerClient == -1;
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
