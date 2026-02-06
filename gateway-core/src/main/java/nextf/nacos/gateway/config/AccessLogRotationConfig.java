package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Access log rotation configuration
 */
public class AccessLogRotationConfig {

    @JsonProperty("policy")
    private String policy = "daily"; // daily, size, both

    @JsonProperty("maxFileSize")
    private String maxFileSize = "100MB";

    @JsonProperty("maxHistory")
    private int maxHistory = 30;

    @JsonProperty("fileNamePattern")
    private String fileNamePattern = "access.%d{yyyy-MM-dd}.log";

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    @Override
    public String toString() {
        return "AccessLogRotationConfig{" +
                "policy='" + policy + '\'' +
                ", maxFileSize='" + maxFileSize + '\'' +
                ", maxHistory=" + maxHistory +
                ", fileNamePattern='" + fileNamePattern + '\'' +
                '}';
    }
}
