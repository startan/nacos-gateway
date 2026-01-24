package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Timeout configuration
 */
public class TimeoutConfig {

    @JsonProperty("connectTimeoutSeconds")
    private int connectTimeoutSeconds = 10;

    @JsonProperty("requestTimeoutSeconds")
    private int requestTimeoutSeconds = 30;

    @JsonProperty("idleTimeoutSeconds")
    private int idleTimeoutSeconds = 60;

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    @Override
    public String toString() {
        return "TimeoutConfig{" +
                "connectTimeoutSeconds=" + connectTimeoutSeconds +
                ", requestTimeoutSeconds=" + requestTimeoutSeconds +
                ", idleTimeoutSeconds=" + idleTimeoutSeconds +
                '}';
    }
}
