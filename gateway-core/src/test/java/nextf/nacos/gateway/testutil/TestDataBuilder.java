package nextf.nacos.gateway.testutil;

import nextf.nacos.gateway.config.*;
import nextf.nacos.gateway.model.Endpoint;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.route.Route;

import java.util.List;
import java.util.ArrayList;

/**
 * Builder for creating test data objects
 * Provides fluent API for building complex test objects with sensible defaults
 */
public class TestDataBuilder {

    /**
     * Build a minimal valid GatewayConfig for testing
     */
    public static class GatewayConfigBuilder {
        private ServerConfig serverConfig;
        private List<RouteConfig> routes = new ArrayList<>();
        private List<BackendConfig> backends = new ArrayList<>();
        private TimeoutConfig timeoutConfig;
        private AccessLogConfig accessLogConfig;
        private ManagementConfig managementConfig;

        public GatewayConfigBuilder() {
            // Set up minimal valid defaults
            withDefaultServer();
            withDefaultBackend();
            withDefaultRoute();
        }

        public GatewayConfigBuilder withDefaultServer() {
            ServerConfig config = new ServerConfig();
            ServerConfig.PortsConfig ports = new ServerConfig.PortsConfig();
            ports.setApiV1(18848);
            ports.setApiV2(19848);
            ports.setApiConsole(18080);
            config.setPorts(ports);

            RateLimitConfig rateLimit = new RateLimitConfig();
            rateLimit.setMaxQps(2000);
            rateLimit.setMaxConnections(10000);
            rateLimit.setMaxQpsPerClient(10);
            rateLimit.setMaxConnectionsPerClient(5);
            config.setRateLimit(rateLimit);

            this.serverConfig = config;
            return this;
        }

        public GatewayConfigBuilder withServer(ServerConfig server) {
            this.serverConfig = server;
            return this;
        }

        public GatewayConfigBuilder withDefaultRoute() {
            RouteConfig route = new RouteConfig();
            route.setHost("*.nacos.io");
            route.setBackend("test-backend");
            this.routes.add(route);
            return this;
        }

        public GatewayConfigBuilder withRoute(String host, String backend) {
            RouteConfig route = new RouteConfig();
            route.setHost(host);
            route.setBackend(backend);
            this.routes.add(route);
            return this;
        }

        public GatewayConfigBuilder withRoutes(List<RouteConfig> routes) {
            this.routes = routes;
            return this;
        }

        public GatewayConfigBuilder withDefaultBackend() {
            BackendConfig backend = new BackendConfig();
            backend.setName("test-backend");
            backend.setLoadBalance("round-robin");

            BackendConfig.BackendPortsConfig ports = new BackendConfig.BackendPortsConfig();
            ports.setApiV1(8848);
            ports.setApiV2(9848);
            ports.setApiConsole(8080);
            backend.setPorts(ports);

            List<EndpointConfig> endpoints = new ArrayList<>();
            EndpointConfig endpoint = new EndpointConfig();
            endpoint.setHost("10.0.0.1");
            endpoint.setPriority(10);
            endpoints.add(endpoint);
            backend.setEndpoints(endpoints);

            this.backends.add(backend);
            return this;
        }

        public GatewayConfigBuilder withBackend(String name, String host, int priority) {
            BackendConfig backend = new BackendConfig();
            backend.setName(name);
            backend.setLoadBalance("round-robin");

            BackendConfig.BackendPortsConfig ports = new BackendConfig.BackendPortsConfig();
            ports.setApiV1(8848);
            ports.setApiV2(9848);
            ports.setApiConsole(8080);
            backend.setPorts(ports);

            List<EndpointConfig> endpoints = new ArrayList<>();
            EndpointConfig endpoint = new EndpointConfig();
            endpoint.setHost(host);
            endpoint.setPriority(priority);
            endpoints.add(endpoint);
            backend.setEndpoints(endpoints);

            this.backends.add(backend);
            return this;
        }

        public GatewayConfigBuilder withBackends(List<BackendConfig> backends) {
            this.backends = backends;
            return this;
        }

        public GatewayConfigBuilder withTimeout(int connectTimeout, int requestTimeout, int idleTimeout) {
            TimeoutConfig timeout = new TimeoutConfig();
            timeout.setConnectTimeoutSeconds(connectTimeout);
            timeout.setRequestTimeoutSeconds(requestTimeout);
            timeout.setIdleTimeoutSeconds(idleTimeout);
            this.timeoutConfig = timeout;
            return this;
        }

        public GatewayConfigBuilder withAccessLog(boolean enabled, String format) {
            AccessLogConfig accessLog = new AccessLogConfig();
            accessLog.setEnabled(enabled);
            accessLog.setFormat(format);
            this.accessLogConfig = accessLog;
            return this;
        }

        public GatewayConfigBuilder withManagement(boolean healthEnabled, String healthPath) {
            ManagementConfig management = new ManagementConfig();
            ManagementConfig.HealthEndpointConfig health = new ManagementConfig.HealthEndpointConfig();
            health.setEnabled(healthEnabled);
            health.setPath(healthPath);
            management.setHealth(health);
            this.managementConfig = management;
            return this;
        }

        public GatewayConfig build() {
            GatewayConfig config = new GatewayConfig();
            config.setServer(serverConfig);
            config.setRoutes(routes);
            config.setBackends(backends);
            config.setTimeout(timeoutConfig);
            config.setAccessLog(accessLogConfig);
            config.setManagement(managementConfig);
            return config;
        }
    }

    /**
     * Build Backend model objects for registry tests
     */
    public static class BackendBuilder {
        private String name;
        private nextf.nacos.gateway.loadbalance.LoadBalancer loadBalancer;
        private List<Endpoint> endpoints = new ArrayList<>();
        private nextf.nacos.gateway.config.BackendConfig backendConfig;

        public BackendBuilder(String name) {
            this.name = name;
            this.loadBalancer = nextf.nacos.gateway.loadbalance.LoadBalancerFactory.create("round-robin");
        }

        public BackendBuilder withLoadBalancer(nextf.nacos.gateway.loadbalance.LoadBalancer loadBalancer) {
            this.loadBalancer = loadBalancer;
            return this;
        }

        public BackendBuilder withEndpoint(Endpoint endpoint) {
            this.endpoints.add(endpoint);
            return this;
        }

        public BackendBuilder withBackendConfig(BackendConfig config) {
            this.backendConfig = config;
            return this;
        }

        public Backend build() {
            return new Backend(name, loadBalancer, endpoints, backendConfig);
        }
    }

    /**
     * Build Route model objects for registry tests
     */
    public static class RouteBuilder {
        private RouteConfig config;

        public RouteBuilder(String host, String backend) {
            this.config = new RouteConfig();
            this.config.setHost(host);
            this.config.setBackend(backend);
        }

        public RouteBuilder withRateLimit(int maxQps, int maxConns, int maxQpsPerClient, int maxConnsPerClient) {
            RateLimitConfig rateLimit = new RateLimitConfig();
            rateLimit.setMaxQps(maxQps);
            rateLimit.setMaxConnections(maxConns);
            rateLimit.setMaxQpsPerClient(maxQpsPerClient);
            rateLimit.setMaxConnectionsPerClient(maxConnsPerClient);
            this.config.setRateLimit(rateLimit);
            return this;
        }

        public Route build() {
            return new Route(config);
        }
    }
}
