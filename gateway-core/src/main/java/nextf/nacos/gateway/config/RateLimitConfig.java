package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rate limit configuration
 */
public class RateLimitConfig {

    @JsonProperty("globalQpsLimit")
    private int globalQpsLimit = 10000;

    @JsonProperty("globalMaxConnections")
    private int globalMaxConnections = 5000;

    @JsonProperty("defaultQpsLimit")
    private int defaultQpsLimit = 1000;

    @JsonProperty("defaultMaxConnections")
    private int defaultMaxConnections = 500;

    public int getGlobalQpsLimit() {
        return globalQpsLimit;
    }

    public void setGlobalQpsLimit(int globalQpsLimit) {
        this.globalQpsLimit = globalQpsLimit;
    }

    public int getGlobalMaxConnections() {
        return globalMaxConnections;
    }

    public void setGlobalMaxConnections(int globalMaxConnections) {
        this.globalMaxConnections = globalMaxConnections;
    }

    public int getDefaultQpsLimit() {
        return defaultQpsLimit;
    }

    public void setDefaultQpsLimit(int defaultQpsLimit) {
        this.defaultQpsLimit = defaultQpsLimit;
    }

    public int getDefaultMaxConnections() {
        return defaultMaxConnections;
    }

    public void setDefaultMaxConnections(int defaultMaxConnections) {
        this.defaultMaxConnections = defaultMaxConnections;
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "globalQpsLimit=" + globalQpsLimit +
                ", globalMaxConnections=" + globalMaxConnections +
                ", defaultQpsLimit=" + defaultQpsLimit +
                ", defaultMaxConnections=" + defaultMaxConnections +
                '}';
    }
}
