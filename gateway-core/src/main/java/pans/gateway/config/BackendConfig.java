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

    @JsonProperty("probe")
    private HealthProbeConfig probe;

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

    public HealthProbeConfig getProbe() {
        return probe;
    }

    public void setProbe(HealthProbeConfig probe) {
        this.probe = probe;
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
                ", probe=" + probe +
                ", endpoints=" + endpoints +
                '}';
    }
}
