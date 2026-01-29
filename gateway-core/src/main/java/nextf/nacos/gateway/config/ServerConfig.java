package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server configuration
 */
public class ServerConfig {

    @JsonProperty("ports")
    private PortsConfig ports;

    @JsonProperty("rateLimit")
    private RateLimitConfig rateLimit;

    public PortsConfig getPorts() {
        return ports;
    }

    public void setPorts(PortsConfig ports) {
        this.ports = ports;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * Get port number for specific port type
     * @param portType the port type (e.g., "apiV1", "apiV2", "apiConsole")
     * @return the port number
     */
    public int getPortForType(String portType) {
        if (ports == null) {
            throw new IllegalStateException("Server ports configuration is missing");
        }
        return ports.getPortForType(portType);
    }

    /**
     * Get all configured ports as a map
     * @return map of port type to port number
     */
    public Map<String, Integer> getAllPorts() {
        if (ports == null) {
            throw new IllegalStateException("Server ports configuration is missing");
        }
        Map<String, Integer> result = new HashMap<>();
        result.put(PortType.API_V1.getConfigName(), ports.apiV1);
        result.put(PortType.API_V2.getConfigName(), ports.apiV2);
        result.put(PortType.API_CONSOLE.getConfigName(), ports.apiConsole);
        return result;
    }

    /**
     * Validate all ports are in valid range and unique
     * @throws IllegalArgumentException if validation fails
     */
    public void validatePorts() {
        if (ports == null) {
            throw new IllegalArgumentException("Server ports configuration is missing");
        }

        Set<Integer> portNumbers = new HashSet<>();
        validatePort(ports.apiV1, "apiV1", portNumbers);
        validatePort(ports.apiV2, "apiV2", portNumbers);
        validatePort(ports.apiConsole, "apiConsole", portNumbers);
    }

    private void validatePort(int port, String portName, Set<Integer> usedPorts) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid " + portName + " port: " + port);
        }
        if (usedPorts.contains(port)) {
            throw new IllegalArgumentException("Duplicate port number: " + port + " for " + portName);
        }
        usedPorts.add(port);
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "ports=" + ports +
                ", rateLimit=" + rateLimit +
                '}';
    }

    /**
     * Ports configuration
     */
    public static class PortsConfig {
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
            return "PortsConfig{" +
                    "apiV1=" + apiV1 +
                    ", apiV2=" + apiV2 +
                    ", apiConsole=" + apiConsole +
                    '}';
        }
    }
}
