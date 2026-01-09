package pans.gateway.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.GatewayConfig;
import pans.gateway.config.ManagementConfig;
import pans.gateway.config.TimeoutConfig;
import pans.gateway.health.HealthCheckManager;
import pans.gateway.loadbalance.EndpointSelector;
import pans.gateway.loadbalance.LoadBalancerFactory;
import pans.gateway.management.HealthEndpoint;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.proxy.ConnectionManager;
import pans.gateway.proxy.GrpcProxyHandler;
import pans.gateway.proxy.HttpProxyHandler;
import pans.gateway.proxy.ProxyConnection;
import pans.gateway.ratelimit.RateLimitManager;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway server
 */
public class GatewayServer {

    private static final Logger log = LoggerFactory.getLogger(GatewayServer.class);

    private final Vertx vertx;
    private final GatewayConfig config;
    private final String configPath;

    private HttpServer server;
    private HttpClient httpClient;
    private HttpClient http2Client;

    // Core components
    private RouteMatcher routeMatcher;
    private Map<String, Backend> backends;
    private EndpointSelector endpointSelector;
    private ConnectionManager connectionManager;
    private HealthCheckManager healthCheckManager;
    private RateLimitManager rateLimitManager;
    private HealthEndpoint healthEndpoint;

    public GatewayServer(Vertx vertx, GatewayConfig config, String configPath) {
        this.vertx = vertx;
        this.config = config;
        this.configPath = configPath;
    }

    public void start() {
        log.info("Starting Nacos Gateway...");

        // Initialize components
        initializeComponents();

        // Create HTTP server
        HttpServerOptions options = createServerOptions();
        server = vertx.createHttpServer(options);

        // Create HTTP clients
        httpClient = vertx.createHttpClient();
        http2Client = vertx.createHttpClient(createHttp2ClientOptions());

        // Request handler
        server.requestHandler(this::handleRequest);

        // Start server
        server.listen(config.getServer().getPort())
            .onSuccess(v -> log.info("Gateway started successfully on port {}", config.getServer().getPort()))
            .onFailure(t -> {
                log.error("Failed to start gateway: {}", t.getMessage());
                throw new RuntimeException("Failed to start gateway", t);
            });
    }

    public void stop() {
        log.info("Stopping gateway...");

        if (connectionManager != null) {
            connectionManager.closeAll();
        }

        if (healthCheckManager != null) {
            healthCheckManager.stopAll();
        }

        if (server != null) {
            server.close()
                .onSuccess(v -> log.info("Gateway stopped"))
                .onFailure(t -> log.error("Error stopping gateway: {}", t.getMessage()));
        }
    }

    private void initializeComponents() {
        log.info("Initializing gateway components...");

        // Initialize connection manager
        connectionManager = new ConnectionManager();

        // Initialize health check manager
        healthCheckManager = new HealthCheckManager(vertx);

        // Initialize rate limit manager
        rateLimitManager = new RateLimitManager(config);

        // Initialize routes and backends
        initializeRoutesAndBackends();

        // Initialize health endpoint
        ManagementConfig mgmtConfig = config.getManagement();
        if (mgmtConfig != null && mgmtConfig.getHealth() != null && mgmtConfig.getHealth().isEnabled()) {
            healthEndpoint = new HealthEndpoint(mgmtConfig.getHealth().getPath());
            log.info("Health endpoint enabled: {}", healthEndpoint.getPath());
        }

        log.info("Gateway components initialized");
    }

    private void initializeRoutesAndBackends() {
        // Create routes
        List<Route> routes = new ArrayList<>();
        if (config.getRoutes() != null) {
            for (var routeConfig : config.getRoutes()) {
                Route route = new Route(routeConfig);
                routes.add(route);
            }
        }

        routeMatcher = new pans.gateway.route.RouteMatcherImpl(routes);
        log.info("Loaded {} routes", routes.size());

        // Create backends
        backends = new HashMap<>();
        if (config.getBackends() != null) {
            for (var backendConfig : config.getBackends()) {
                // Create endpoints
                List<Endpoint> endpoints = new ArrayList<>();
                for (var endpointConfig : backendConfig.getEndpoints()) {
                    Endpoint endpoint = new Endpoint(endpointConfig);
                    endpoints.add(endpoint);
                }

                // Create load balancer
                var loadBalancer = LoadBalancerFactory.create(backendConfig.getLoadBalance());

                // Create backend
                Backend backend = new Backend(backendConfig.getName(), loadBalancer, endpoints);
                backends.put(backend.getName(), backend);

                // Start health checking
                healthCheckManager.startBackendChecking(backendConfig, endpoints);
            }
        }

        endpointSelector = new pans.gateway.loadbalance.EndpointSelector();
        log.info("Loaded {} backends", backends.size());
    }

    private HttpServerOptions createServerOptions() {
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(config.getServer().getPort());
        options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));

        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null) {
            options.setIdleTimeout(timeout.getIdleTimeoutSeconds());
        }

        return options;
    }

    private io.vertx.core.http.HttpClientOptions createHttp2ClientOptions() {
        io.vertx.core.http.HttpClientOptions options = new io.vertx.core.http.HttpClientOptions();
        options.setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false)
                .setAlpnVersions(List.of(HttpVersion.HTTP_2))
                .setUseAlpn(false)
                .setSsl(false);

        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null) {
            options.setConnectTimeout(timeout.getConnectTimeoutSeconds() * 1000);
            options.setIdleTimeout(timeout.getIdleTimeoutSeconds());
        }

        return options;
    }

    private void handleRequest(HttpServerRequest request) {
        HostAndPort hostAndPort = request.authority();
        String path = request.path();

        log.debug("Received request: {} {}", request.method(), request.uri());

        // Check health endpoint
        if (healthEndpoint != null && healthEndpoint.matches(path)) {
            healthEndpoint.handle(request);
            return;
        }

        // Match route
        var routeOpt = routeMatcher.match(hostAndPort.host(), path);
        if (routeOpt.isEmpty()) {
            log.warn("No route matched for host: {}, path: {}", hostAndPort.host(), path);
            request.response().setStatusCode(404).end("Not Found");
            return;
        }

        Route route = routeOpt.get();

        // Check rate limit
        String routeId = route.getId();
        if (!rateLimitManager.tryAcquire(routeId)) {
            log.warn("Rate limit exceeded for route: {}", routeId);
            request.response().setStatusCode(429).end("Too Many Requests");
            return;
        }

        // Select backend
        Backend backend = backends.get(route.getBackendName());
        if (backend == null) {
            log.error("Backend not found: {}", route.getBackendName());
            request.response().setStatusCode(503).end("Service Unavailable - Backend not found");
            rateLimitManager.release(routeId);
            return;
        }

        // Select endpoint
        Endpoint endpoint = endpointSelector.select(backend);
        if (endpoint == null) {
            log.error("No healthy endpoint for backend: {}", backend.getName());
            request.response().setStatusCode(503).end("Service Unavailable - No healthy endpoints");
            rateLimitManager.release(routeId);
            return;
        }

        // Create proxy connection
        ProxyConnection proxyConnection = new ProxyConnection(request, route, endpoint);
        connectionManager.addConnection(request, proxyConnection);

        // Notify load balancer
        backend.getLoadBalancer().onConnectionOpen(endpoint);

        // Handle proxy
        try {
            if (GrpcProxyHandler.isGrpcRequest(request)) {
                // gRPC request
                GrpcProxyHandler grpcHandler = new GrpcProxyHandler(
                        http2Client,
                        endpoint,
                        config.getTimeout()
                );
                grpcHandler.handle(request);
            } else {
                // HTTP/1 or HTTP/2 request
                HttpProxyHandler httpHandler = new HttpProxyHandler(
                        http2Client,
                        endpoint,
                        config.getTimeout()
                );
                httpHandler.handle(request);
            }

            // Setup cleanup
            request.response().endHandler(v -> {
                connectionManager.removeConnection(request);
                backend.getLoadBalancer().onConnectionClose(endpoint);
                rateLimitManager.release(routeId);
            });

        } catch (Exception e) {
            log.error("Error proxying request: {}", e.getMessage(), e);
            if (!request.response().ended()) {
                request.response().setStatusCode(500).end("Internal Server Error");
            }
            connectionManager.removeConnection(request);
            backend.getLoadBalancer().onConnectionClose(endpoint);
            rateLimitManager.release(routeId);
        }
    }

    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }

    public Map<String, Backend> getBackends() {
        return backends;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public HealthCheckManager getHealthCheckManager() {
        return healthCheckManager;
    }

    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }
}
