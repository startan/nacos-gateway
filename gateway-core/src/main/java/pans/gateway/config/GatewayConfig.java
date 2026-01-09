package pans.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Root gateway configuration
 */
public class GatewayConfig {

    @JsonProperty("server")
    private ServerConfig server;

    @JsonProperty("routes")
    private List<RouteConfig> routes;

    @JsonProperty("backends")
    private List<BackendConfig> backends;

    @JsonProperty("rateLimit")
    private RateLimitConfig rateLimit;

    @JsonProperty("timeout")
    private TimeoutConfig timeout;

    @JsonProperty("logging")
    private LoggingConfig logging;

    @JsonProperty("management")
    private ManagementConfig management;

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public List<RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteConfig> routes) {
        this.routes = routes;
    }

    public List<BackendConfig> getBackends() {
        return backends;
    }

    public void setBackends(List<BackendConfig> backends) {
        this.backends = backends;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutConfig timeout) {
        this.timeout = timeout;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }

    public ManagementConfig getManagement() {
        return management;
    }

    public void setManagement(ManagementConfig management) {
        this.management = management;
    }

    @Override
    public String toString() {
        return "GatewayConfig{" +
                "server=" + server +
                ", routes=" + routes +
                ", backends=" + backends +
                ", rateLimit=" + rateLimit +
                ", timeout=" + timeout +
                ", logging=" + logging +
                ", management=" + management +
                '}';
    }
}
