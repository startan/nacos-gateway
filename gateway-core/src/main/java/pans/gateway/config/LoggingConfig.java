package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Logging configuration
 */
public class LoggingConfig {

    @JsonProperty("level")
    private String level = "INFO";

    @JsonProperty("verbose")
    private boolean verbose = false;

    @JsonProperty("format")
    private String format = "text";

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "LoggingConfig{" +
                "level='" + level + '\'' +
                ", verbose=" + verbose +
                ", format='" + format + '\'' +
                '}';
    }
}
