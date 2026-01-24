package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Management endpoint configuration
 */
public class ManagementConfig {

    @JsonProperty("health")
    private HealthEndpointConfig health;

    public static class HealthEndpointConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;

        @JsonProperty("path")
        private String path = "/health";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return "HealthEndpointConfig{" +
                    "enabled=" + enabled +
                    ", path='" + path + '\'' +
                    '}';
        }
    }

    public HealthEndpointConfig getHealth() {
        return health;
    }

    public void setHealth(HealthEndpointConfig health) {
        this.health = health;
    }

    @Override
    public String toString() {
        return "ManagementConfig{" +
                "health=" + health +
                '}';
    }
}
