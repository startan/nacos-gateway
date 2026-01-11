package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Health probe configuration
 */
public class HealthProbeConfig {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("type")
    private String type = "http";  // "http" or "tcp"

    @JsonProperty("path")
    private String path = "/health";

    @JsonProperty("periodSeconds")
    private int periodSeconds = 10;

    @JsonProperty("timeoutSeconds")
    private int timeoutSeconds = 1;

    @JsonProperty("successThreshold")
    private int successThreshold = 1;

    @JsonProperty("failureThreshold")
    private int failureThreshold = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPeriodSeconds() {
        return periodSeconds;
    }

    public void setPeriodSeconds(int periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getSuccessThreshold() {
        return successThreshold;
    }

    public void setSuccessThreshold(int successThreshold) {
        this.successThreshold = successThreshold;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    @Override
    public String toString() {
        return "HealthProbeConfig{" +
                "enabled=" + enabled +
                ", type='" + type + '\'' +
                ", path='" + path + '\'' +
                ", periodSeconds=" + periodSeconds +
                ", timeoutSeconds=" + timeoutSeconds +
                ", successThreshold=" + successThreshold +
                ", failureThreshold=" + failureThreshold +
                '}';
    }
}
