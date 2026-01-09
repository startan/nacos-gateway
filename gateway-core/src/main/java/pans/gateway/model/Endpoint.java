package pans.gateway.model;

import pans.gateway.config.EndpointConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Endpoint model
 */
public class Endpoint {

    private final String host;
    private final int port;
    private final int priority;
    private final AtomicBoolean healthy;

    public Endpoint(EndpointConfig config) {
        this.host = config.getHost();
        this.port = config.getPort();
        this.priority = config.getPriority();
        this.healthy = new AtomicBoolean(true);
    }

    public Endpoint(String host, int port, int priority) {
        this.host = host;
        this.port = port;
        this.priority = priority;
        this.healthy = new AtomicBoolean(true);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return port == endpoint.port && priority == endpoint.priority &&
                host.equals(endpoint.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        result = 31 * result + priority;
        return result;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", priority=" + priority +
                ", healthy=" + healthy.get() +
                '}';
    }
}
