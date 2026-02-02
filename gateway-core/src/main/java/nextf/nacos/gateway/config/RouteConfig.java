package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Route configuration
 */
public class RouteConfig {

    @JsonProperty("host")
    private String host;

    @JsonProperty("backend")
    private String backend;

    @JsonProperty("rateLimit")
    private RateLimitConfig rateLimit = new RateLimitConfig();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public String toString() {
        return "RouteConfig{" +
                "host='" + host + '\'' +
                ", backend='" + backend + '\'' +
                ", rateLimit=" + rateLimit +
                '}';
    }
}
