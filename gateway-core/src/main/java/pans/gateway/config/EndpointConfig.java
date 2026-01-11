package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Endpoint configuration
 * Port is now configured at BackendConfig level
 */
public class EndpointConfig {

    @JsonProperty("host")
    private String host;

    @JsonProperty("priority")
    private int priority = 10;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "EndpointConfig{" +
                "host='" + host + '\'' +
                ", priority=" + priority +
                '}';
    }
}
