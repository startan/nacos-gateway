package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Access log output configuration
 */
public class AccessLogOutputConfig {

    @JsonProperty("path")
    private String path = "logs/access.log";

    @JsonProperty("encoding")
    private String encoding = "UTF-8";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public String toString() {
        return "AccessLogOutputConfig{" +
                "path='" + path + '\'' +
                ", encoding='" + encoding + '\'' +
                '}';
    }
}
