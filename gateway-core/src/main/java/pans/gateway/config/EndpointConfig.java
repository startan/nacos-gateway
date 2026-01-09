package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Endpoint configuration
 */
public class EndpointConfig {

    @JsonProperty("host")
    private String host;

    @JsonProperty("port")
    private int port;

    @JsonProperty("priority")
    private int priority = 1;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
                ", port=" + port +
                ", priority=" + priority +
                '}';
    }
}
