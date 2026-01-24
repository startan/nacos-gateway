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

    @Override
    public String toString() {
        return "RouteConfig{" +
                "host='" + host + '\'' +
                ", backend='" + backend + '\'' +
                '}';
    }
}
