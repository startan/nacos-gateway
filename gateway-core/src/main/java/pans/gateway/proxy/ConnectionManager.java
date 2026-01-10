package pans.gateway.proxy;

import io.vertx.core.http.HttpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection manager for tracking active ProxyConnections
 */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<HttpConnection, ProxyConnection> connections = new ConcurrentHashMap<>();

    /**
     * Add proxy connection
     */
    public void addConnection(HttpConnection clientConnection, ProxyConnection proxyConnection) {
        connections.put(clientConnection, proxyConnection);
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
            proxyConnection.close();  // ProxyConnection handles all resource cleanup
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
     * Disconnect connections that don't match the new route table
     */
    public void disconnectInvalidConnections(RouteMatcher newRouteMatcher) {
        log.info("Checking {} connections for route validity", connections.size());

        int disconnected = 0;
        for (Map.Entry<HttpConnection, ProxyConnection> entry : connections.entrySet()) {
            HttpConnection connection = entry.getKey();
            ProxyConnection proxyConnection = entry.getValue();
            Route route = proxyConnection.getRoute();

            // Check if route still exists
            boolean routeExists = newRouteMatcher.match(route.getHostPattern(), route.getPathPattern()).isPresent();

            if (!routeExists) {
                // Close the connection
                try {
                    connection.close();
                    log.debug("Connection {}: Closed due to route configuration change", connection);
                } catch (Exception e) {
                    log.warn("Error closing connection: {}", e.getMessage());
                }
                disconnected++;
            }
        }

        if (disconnected > 0) {
            log.info("Disconnected {} connections due to route configuration change", disconnected);
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
