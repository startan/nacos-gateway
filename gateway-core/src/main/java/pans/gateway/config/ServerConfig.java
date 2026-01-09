package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server configuration
 */
public class ServerConfig {

    @JsonProperty("port")
    private int port = 9848;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                '}';
    }
}
