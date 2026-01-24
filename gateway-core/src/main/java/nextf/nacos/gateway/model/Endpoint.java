package nextf.nacos.gateway.model;

import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.config.EndpointConfig;
import nextf.nacos.gateway.config.PortType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Endpoint model
 * Contains host and three port types (apiV1, apiV2, apiConsole)
 */
public class Endpoint {

    private final String host;
    private final int apiV1Port;
    private final int apiV2Port;
    private final int apiConsolePort;
    private final int priority;
    private final AtomicBoolean healthy;

    /**
     * Static factory method to build an Endpoint from configuration
     * @param config Endpoint configuration
     * @param ports Port configuration
     * @return New Endpoint instance
     */
    public static Endpoint from(EndpointConfig config, BackendConfig.BackendPortsConfig ports) {
        return new Endpoint(config, ports);
    }

    /**
     * Constructor with ports configuration
     * Used when creating Endpoint from configuration
     */
    public Endpoint(EndpointConfig config, BackendConfig.BackendPortsConfig ports) {
        this.host = config.getHost();
        this.apiV1Port = ports.getApiV1();
        this.apiV2Port = ports.getApiV2();
        this.apiConsolePort = ports.getApiConsole();
        this.priority = config.getPriority();
        this.healthy = new AtomicBoolean(true);
    }

    public String getHost() {
        return host;
    }

    public int getApiV1Port() {
        return apiV1Port;
    }

    public int getApiV2Port() {
        return apiV2Port;
    }

    public int getApiConsolePort() {
        return apiConsolePort;
    }

    /**
     * Get port for specific port type
     * @param portType the port type
     * @return the port number
     */
    public int getPortForType(PortType portType) {
        return switch (portType) {
            case API_V1 -> apiV1Port;
            case API_V2 -> apiV2Port;
            case API_CONSOLE -> apiConsolePort;
        };
    }

    public int getPriority() {
        return priority;
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public void setHealthy(boolean healthy) {
        this.healthy.set(healthy);
    }

    /**
     * Get address for specific port type
     * @param portType the port type
     * @return the address in format "host:port"
     */
    public String getAddress(PortType portType) {
        return host + ":" + getPortForType(portType);
    }

    /**
     * @deprecated Use {@link #getAddress(PortType)} instead
     * Returns the address for apiV1 port by default
     */
    @Deprecated
    public String getAddress() {
        return getAddress(PortType.API_V1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return apiV1Port == endpoint.apiV1Port &&
                apiV2Port == endpoint.apiV2Port &&
                apiConsolePort == endpoint.apiConsolePort &&
                priority == endpoint.priority &&
                host.equals(endpoint.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + apiV1Port;
        result = 31 * result + apiV2Port;
        result = 31 * result + apiConsolePort;
        result = 31 * result + priority;
        return result;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "host='" + host + '\'' +
                ", apiV1Port=" + apiV1Port +
                ", apiV2Port=" + apiV2Port +
                ", apiConsolePort=" + apiConsolePort +
                ", priority=" + priority +
                ", healthy=" + healthy.get() +
                '}';
    }
}
