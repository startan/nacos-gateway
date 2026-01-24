package nextf.nacos.gateway.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.PortType;
import nextf.nacos.gateway.config.TimeoutConfig;
import nextf.nacos.gateway.loadbalance.EndpointSelector;
import nextf.nacos.gateway.management.HealthEndpoint;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.model.Endpoint;
import nextf.nacos.gateway.proxy.ConnectionManager;
import nextf.nacos.gateway.proxy.GrpcProxyHandler;
import nextf.nacos.gateway.proxy.HttpProxyHandler;
import nextf.nacos.gateway.proxy.ProxyConnection;
import nextf.nacos.gateway.ratelimit.LimitExceededException;
import nextf.nacos.gateway.ratelimit.RateLimitManager;
import nextf.nacos.gateway.registry.GatewayRegistry;
import nextf.nacos.gateway.route.Route;

import java.util.List;

/**
 * Gateway server for a specific port type
 * Uses shared components (RouteMatcher, Backends, etc.) from GatewayServerManager
 */
public class GatewayServer {

    private static final Logger log = LoggerFactory.getLogger(GatewayServer.class);

    private final Vertx vertx;
    private final GatewayConfig config;
    private final PortType portType;
    private final int listeningPort;

    private HttpServer server;

    // Shared components (injected from GatewayServerManager)
    private final GatewayRegistry registry;
    private final EndpointSelector endpointSelector;
    private final ConnectionManager connectionManager;
    private final RateLimitManager rateLimitManager;
    private final HealthEndpoint healthEndpoint;

    /**
     * Constructor for multi-port gateway
     * @param vertx Vert.x instance
     * @param config Gateway configuration
     * @param portType The port type this server handles
     * @param port The port number to listen on
     * @param registry Shared gateway registry
     * @param endpointSelector Shared endpoint selector
     * @param connectionManager Shared connection manager
     * @param rateLimitManager Shared rate limit manager
     * @param healthEndpoint Shared health endpoint
     */
    public GatewayServer(
            Vertx vertx,
            GatewayConfig config,
            PortType portType,
            int port,
            GatewayRegistry registry,
            EndpointSelector endpointSelector,
            ConnectionManager connectionManager,
            RateLimitManager rateLimitManager,
            HealthEndpoint healthEndpoint) {
        this.vertx = vertx;
        this.config = config;
        this.portType = portType;
        this.listeningPort = port;
        this.registry = registry;
        this.endpointSelector = endpointSelector;
        this.connectionManager = connectionManager;
        this.rateLimitManager = rateLimitManager;
        this.healthEndpoint = healthEndpoint;
    }

    public void start(Handler<HttpServer> handler) {
        log.info("Starting {} server on port {}...", portType.getDescription(), listeningPort);

        // Create HTTP server
        HttpServerOptions options = createServerOptions();
        server = vertx.createHttpServer(options);

        // Request handler
        server.requestHandler(this::handleRequest);

        // Start server
        server.listen(listeningPort)
            .onSuccess(v -> {
                log.info("{} server started on port {}", portType.getDescription(), listeningPort);
                handler.handle(v);
            })
            .onFailure(t -> {
                log.error("Failed to start {} server: {}", portType.getDescription(), t.getMessage());
                throw new RuntimeException("Failed to start server", t);
            });
    }

    public void stop() {
        log.info("Stopping {} server on port {}...", portType.getDescription(), listeningPort);

        if (server != null) {
            server.close()
                .onSuccess(v -> log.info("{} server on port {} stopped", portType.getDescription(), listeningPort))
                .onFailure(t -> log.error("Error stopping {} server: {}", portType.getDescription(), t.getMessage()));
        }
    }

    private HttpServerOptions createServerOptions() {
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(listeningPort);
        options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));

        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null) {
            options.setIdleTimeout(timeout.getIdleTimeoutSeconds());
        }

        return options;
    }

    private io.vertx.core.http.HttpClientOptions createHttp1ClientOptions() {
        io.vertx.core.http.HttpClientOptions options = new io.vertx.core.http.HttpClientOptions();
        options.setProtocolVersion(HttpVersion.HTTP_1_1)
                .setSsl(false);

        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null) {
            options.setConnectTimeout(timeout.getConnectTimeoutSeconds() * 1000);
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
        HttpConnection connection = request.connection();
        HostAndPort hostAndPort = request.authority();
        String path = request.path();
        String clientIp = request.remoteAddress().host();

        log.debug("Received request on {} port: {} {}", portType.getConfigName(), request.method(), request.uri());

        // Check health endpoint
        if (healthEndpoint != null && healthEndpoint.matches(path)) {
            healthEndpoint.handle(request);
            return;
        }

        // Match route
        var routeOpt = registry.getRouteMatcher().match(hostAndPort.host());
        if (routeOpt.isEmpty()) {
            log.warn("No route matched for host: {}", hostAndPort.host());
            request.response().setStatusCode(404).end("Not Found");
            return;
        }

        Route route = routeOpt.get();
        String backendName = route.getBackendName();

        // Check rate limit (QPS only, no connection check)
        if (!rateLimitManager.tryAcquire(backendName, clientIp)) {
            log.warn("Rate limit exceeded for client: {}", clientIp);
            request.response().setStatusCode(429).end("Too Many Requests");
            return;
        }

        // Connection-level: Get or create ProxyConnection
        ProxyConnection proxyConnection = connectionManager.getConnection(connection);

        if (proxyConnection == null) {
            // First request on this connection - perform connection-level initialization

            // 1. Select backend and endpoint (load balancing)
            Backend backend = registry.getBackend(backendName);
            if (backend == null) {
                log.error("Backend not found: {}", backendName);
                request.response().setStatusCode(503).end("Service Unavailable - Backend not found");
                return;
            }

            Endpoint endpoint = endpointSelector.select(backend);
            if (endpoint == null) {
                log.error("No healthy endpoint for backend: {}", backend.getName());
                request.response().setStatusCode(503).end("Service Unavailable - No healthy endpoints");
                return;
            }

            HttpClientOptions httpClientOptions = switch (portType) {
                case API_V1, API_CONSOLE -> createHttp1ClientOptions();
                case API_V2 -> createHttp2ClientOptions();
            };
            // 2. Create dedicated HttpClient
            HttpClient clientHttpClient = vertx.createHttpClient(httpClientOptions);

            // 3. Notify load balancer (connection level)
            backend.getLoadBalancer().onConnectionOpen(endpoint);

            // 4. Create ProxyConnection with all resources including portType
            proxyConnection = new ProxyConnection(connection, route, endpoint, backend, clientHttpClient, portType, clientIp);

            // 5. Add connection (ConnectionManager will handle close/exception handlers)
            try {
                connectionManager.addConnection(proxyConnection);
                log.debug("Connection {}: Created ProxyConnection with {} for {} port",
                        connection, endpoint.getAddress(), portType.getConfigName());
            } catch (LimitExceededException e) {
                log.warn("Connection limit exceeded for client: {}", proxyConnection.getClientIp());
                request.response().setStatusCode(429).end("Too Many Connections");
                return;
            }
        }

        // Use the connection's HttpClient and get the correct port for this portType
        try {
            Endpoint endpoint = proxyConnection.getEndpoint();
            String backendHost = endpoint.getHost();
            int backendPort = endpoint.getPortForType(portType);

            switch (portType) {
                case API_V1, API_CONSOLE -> {
                    HttpProxyHandler httpHandler = new HttpProxyHandler(
                            proxyConnection.getHttpClient(),
                            backendHost,
                            backendPort,
                            config.getTimeout()
                    );
                    httpHandler.handle(request);
                }
                case API_V2 -> {
                    GrpcProxyHandler grpcHandler = new GrpcProxyHandler(
                            proxyConnection.getHttpClient(),
                            backendHost,
                            backendPort,
                            config.getTimeout()
                    );
                    grpcHandler.handle(request);
                }
            }
        } catch (Exception e) {
            log.error("Error proxying request: {}", e.getMessage(), e);
            if (!request.response().ended()) {
                request.response().setStatusCode(500).end("Internal Server Error");
            }
        }
    }

    // Getters for shared components (for compatibility)
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }

    public PortType getPortType() {
        return portType;
    }

    public int getListeningPort() {
        return listeningPort;
    }
}
