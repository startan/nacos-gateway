package pans.gateway.config;

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
    private BackendRateLimitConfig rateLimit;

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

    public BackendRateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(BackendRateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public List<EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Check if backend has custom rate limit configuration
     */
    public boolean hasCustomRateLimit() {
        return rateLimit != null;
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

    /**
     * Backend-level rate limit configuration
     */
    public static class BackendRateLimitConfig {
        @JsonProperty("maxQps")
        private int maxQps;

        @JsonProperty("maxConnections")
        private int maxConnections;

        @JsonProperty("maxQpsPerClient")
        private int maxQpsPerClient;

        @JsonProperty("maxConnectionsPerClient")
        private int maxConnectionsPerClient;

        public int getMaxQps() {
            return maxQps;
        }

        public void setMaxQps(int maxQps) {
            this.maxQps = maxQps;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getMaxQpsPerClient() {
            return maxQpsPerClient;
        }

        public void setMaxQpsPerClient(int maxQpsPerClient) {
            this.maxQpsPerClient = maxQpsPerClient;
        }

        public int getMaxConnectionsPerClient() {
            return maxConnectionsPerClient;
        }

        public void setMaxConnectionsPerClient(int maxConnectionsPerClient) {
            this.maxConnectionsPerClient = maxConnectionsPerClient;
        }

        @Override
        public String toString() {
            return "BackendRateLimitConfig{" +
                    "maxQps=" + maxQps +
                    ", maxConnections=" + maxConnections +
                    ", maxQpsPerClient=" + maxQpsPerClient +
                    ", maxConnectionsPerClient=" + maxConnectionsPerClient +
                    '}';
        }
    }
}
