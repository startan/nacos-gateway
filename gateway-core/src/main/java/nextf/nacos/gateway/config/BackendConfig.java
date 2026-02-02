package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Backend configuration
 */
public class BackendConfig {

    @JsonProperty("name")
    private String name;

    @JsonProperty("loadBalance")
    private String loadBalance = "round-robin";

    @JsonProperty("ports")
    private BackendPortsConfig ports;

    @JsonProperty("probe")
    private HealthProbeConfig probe;

    @JsonProperty("rateLimit")
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @JsonProperty("endpoints")
    private List<EndpointConfig> endpoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(String loadBalance) {
        this.loadBalance = loadBalance;
    }

    public BackendPortsConfig getPorts() {
        return ports;
    }

    public void setPorts(BackendPortsConfig ports) {
        this.ports = ports;
    }

    public HealthProbeConfig getProbe() {
        return probe;
    }

    public void setProbe(HealthProbeConfig probe) {
        this.probe = probe;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public List<EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public String toString() {
        return "BackendConfig{" +
                "name='" + name + '\'' +
                ", loadBalance='" + loadBalance + '\'' +
                ", ports=" + ports +
                ", probe=" + probe +
                ", rateLimit=" + rateLimit +
                ", endpoints=" + endpoints +
                '}';
    }

    /**
     * Backend ports configuration
     */
    public static class BackendPortsConfig {
        @JsonProperty("apiV1")
        private int apiV1 = 8848;

        @JsonProperty("apiV2")
        private int apiV2 = 9848;

        @JsonProperty("apiConsole")
        private int apiConsole = 8080;

        public int getApiV1() {
            return apiV1;
        }

        public void setApiV1(int apiV1) {
            this.apiV1 = apiV1;
        }

        public int getApiV2() {
            return apiV2;
        }

        public void setApiV2(int apiV2) {
            this.apiV2 = apiV2;
        }

        public int getApiConsole() {
            return apiConsole;
        }

        public void setApiConsole(int apiConsole) {
            this.apiConsole = apiConsole;
        }

        /**
         * Get port number for specific port type
         * @param portType the port type config name
         * @return the port number
         */
        public int getPortForType(String portType) {
            return switch (portType) {
                case "apiV1" -> apiV1;
                case "apiV2" -> apiV2;
                case "apiConsole" -> apiConsole;
                default -> throw new IllegalArgumentException("Unknown port type: " + portType);
            };
        }

        @Override
        public String toString() {
            return "BackendPortsConfig{" +
                    "apiV1=" + apiV1 +
                    ", apiV2=" + apiV2 +
                    ", apiConsole=" + apiConsole +
                    '}';
        }
    }
}
