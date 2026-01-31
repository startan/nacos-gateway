package nextf.nacos.gateway.proxy;

import io.vertx.core.http.HttpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.ratelimit.LimitExceededException;
import nextf.nacos.gateway.ratelimit.RateLimitManager;
import nextf.nacos.gateway.route.Route;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection manager for tracking active ProxyConnections
 */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<HttpConnection, ProxyConnection> connections = new ConcurrentHashMap<>();
    private final RateLimitManager rateLimitManager;

    /**
     * Constructor with RateLimitManager
     * @param rateLimitManager rate limit manager for releasing connection permits
     */
    public ConnectionManager(RateLimitManager rateLimitManager) {
        this.rateLimitManager = rateLimitManager;
    }

    /**
     * Add proxy connection
     */
    public void addConnection(ProxyConnection proxyConnection) throws LimitExceededException {
        // Check connection limit (should be checked once per TCP connection)
        if (!rateLimitManager.tryAcquireConnection(
                proxyConnection.getBackend().getName(),
                proxyConnection.getClientIp(),
                proxyConnection.getRoute().getId())) {
            throw new LimitExceededException("Too Many Connections");
        }

        HttpConnection clientConnection = proxyConnection.getClientConnection();
        connections.put(proxyConnection.getClientConnection(), proxyConnection);
        // Register cleanup handlers (connection level)
        HttpConnection connection = proxyConnection.getClientConnection();
        connection.closeHandler(v -> {
            log.debug("Connection {}: Normally closed", connection);
            this.removeConnection(connection);
        });

        connection.exceptionHandler(t -> {
            log.debug("Connection {}: Abnormally closed - {}", connection, t.getMessage());
            this.removeConnection(connection);
        });
        log.debug("Added connection: {} -> {}, total: {}",
                clientConnection,
                proxyConnection.getEndpoint().getAddress(),
                connections.size());
    }

    /**
     * Remove connection and cleanup all resources via ProxyConnection.close()
     */
    public void removeConnection(HttpConnection connection) {
        ProxyConnection proxyConnection = connections.remove(connection);
        if (proxyConnection != null) {
            // Release connection permit from rate limiter
            rateLimitManager.releaseConnection(
                proxyConnection.getBackend().getName(),
                proxyConnection.getClientIp(),
                proxyConnection.getRoute() != null ? proxyConnection.getRoute().getId() : null
            );

            // Close all resources (HttpClient, notify load balancer, etc.)
            proxyConnection.close();

            log.debug("Connection {}: Removed proxy connection, duration: {}ms, total: {}",
                    connection,
                    proxyConnection.getDuration(),
                    connections.size());
        }
    }

    /**
     * Get proxy connection for this HttpConnection
     */
    public ProxyConnection getConnection(HttpConnection connection) {
        return connections.get(connection);
    }

    /**
     * Disconnect connections when configuration changes
     * Checks both route and backend validity against new configuration
     *
     * @param newRoutes New routes from updated configuration
     * @param newBackends New backends from updated configuration
     */
    public void disconnectInvalidConnections(
            Map<String, Route> newRoutes,
            Map<String, Backend> newBackends) {
        log.info("Checking {} connections for configuration validity", connections.size());

        int disconnected = 0;
        for (Map.Entry<HttpConnection, ProxyConnection> entry : connections.entrySet()) {
            HttpConnection connection = entry.getKey();
            ProxyConnection pc = entry.getValue();

            Route connectionRoute = pc.getRoute();        // Route object held by connection (old)
            Backend connectionBackend = pc.getBackend();   // Backend object held by connection (old)

            // Check 1: Route still exists in new config
            Route newRoute = newRoutes.get(connectionRoute.getId());
            if (newRoute == null) {
                disconnectConnection(connection, "Route removed: " + connectionRoute.getHostPattern());
                disconnected++;
                continue;
            }

            // Check 2: Route's backend target hasn't changed
            if (!Objects.equals(connectionRoute.getBackendName(), newRoute.getBackendName())) {
                disconnectConnection(connection,
                        String.format("Route backend changed: %s -> %s (was: %s)",
                                connectionRoute.getHostPattern(),
                                newRoute.getBackendName(),
                                connectionRoute.getBackendName()));
                disconnected++;
                continue;
            }

            // Check 3: Backend still exists in new config
            Backend newBackend = newBackends.get(connectionBackend.getName());
            if (newBackend == null) {
                disconnectConnection(connection, "Backend removed: " + connectionBackend.getName());
                disconnected++;
                continue;
            }

            // Check 4: Backend critical configuration hasn't changed
            if (hasBackendCriticalConfigChanged(connectionBackend, newBackend)) {
                disconnectConnection(connection,
                        "Backend critical configuration changed: " + connectionBackend.getName());
                disconnected++;
            }
        }

        if (disconnected > 0) {
            log.info("Disconnected {} connections due to configuration change", disconnected);
        }
    }

    /**
     * Check if backend critical configuration changed (endpoints or ports)
     * Compares old backend (held by connection) with new backend (from config)
     */
    private boolean hasBackendCriticalConfigChanged(Backend oldBackend, Backend newBackend) {
        // Compare endpoints list
        var oldEndpoints = oldBackend.getEndpoints();
        var newEndpoints = newBackend.getEndpoints();

        if (oldEndpoints.size() != newEndpoints.size()) {
            log.debug("Endpoint count changed for backend {}: {} -> {}",
                    oldBackend.getName(), oldEndpoints.size(), newEndpoints.size());
            return true;
        }

        // Endpoint.equals() compares: host, apiV1Port, apiV2Port, apiConsolePort, priority
        if (!new HashSet<>(oldEndpoints).equals(new HashSet<>(newEndpoints))) {
            log.debug("Endpoint configuration changed for backend {}", oldBackend.getName());
            return true;
        }

        // Compare port configuration
        BackendConfig oldConfig = oldBackend.getBackendConfig();
        BackendConfig newConfig = newBackend.getBackendConfig();

        if (oldConfig != null && newConfig != null) {
            BackendConfig.BackendPortsConfig oldPorts = oldConfig.getPorts();
            BackendConfig.BackendPortsConfig newPorts = newConfig.getPorts();

            if (oldPorts != null && newPorts != null) {
                if (!Objects.equals(oldPorts.getApiV1(), newPorts.getApiV1()) ||
                        !Objects.equals(oldPorts.getApiV2(), newPorts.getApiV2()) ||
                        !Objects.equals(oldPorts.getApiConsole(), newPorts.getApiConsole())) {
                    log.debug("Port configuration changed for backend {}", oldBackend.getName());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Disconnect a single connection with reason logging
     */
    private void disconnectConnection(HttpConnection connection, String reason) {
        try {
            connection.close();
            log.debug("Connection {}: Closed due to {}", connection, reason);
        } catch (Exception e) {
            log.warn("Error closing connection: {}", e.getMessage());
        }
    }

    /**
     * Close all connections
     */
    public void closeAll() {
        log.info("Closing all {} connections", connections.size());
        connections.keySet().forEach(connection -> {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Error closing connection", e);
            }
        });
        connections.clear();
    }

    public int getConnectionCount() {
        return connections.size();
    }
}
