package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Access log configuration
 */
public class AccessLogConfig {

    @JsonProperty("enabled")
    private boolean enabled = false;

    @JsonProperty("format")
    private String format = "pattern"; // pattern or json

    @JsonProperty("pattern")
    private String pattern = "%h - - [%t] \"%m %U %H\" %s %b %D \"%{User-Agent}i\" \"%{Referer}i\"%n";

    @JsonProperty("output")
    private AccessLogOutputConfig output = new AccessLogOutputConfig();

    @JsonProperty("rotation")
    private AccessLogRotationConfig rotation = new AccessLogRotationConfig();

    @JsonProperty("async")
    private AccessLogAsyncConfig async = new AccessLogAsyncConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public AccessLogOutputConfig getOutput() {
        return output;
    }

    public void setOutput(AccessLogOutputConfig output) {
        this.output = output;
    }

    public AccessLogRotationConfig getRotation() {
        return rotation;
    }

    public void setRotation(AccessLogRotationConfig rotation) {
        this.rotation = rotation;
    }

    public AccessLogAsyncConfig getAsync() {
        return async;
    }

    public void setAsync(AccessLogAsyncConfig async) {
        this.async = async;
    }

    @Override
    public String toString() {
        return "AccessLogConfig{" +
                "enabled=" + enabled +
                ", format='" + format + '\'' +
                ", pattern='" + pattern + '\'' +
                ", output=" + output +
                ", rotation=" + rotation +
                ", async=" + async +
                '}';
    }
}
