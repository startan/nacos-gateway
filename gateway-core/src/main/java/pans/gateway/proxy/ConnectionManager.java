package pans.gateway.proxy;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection manager for tracking active connections
 */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<HttpServerRequest, ProxyConnection> connections = new ConcurrentHashMap<>();

    public void addConnection(HttpServerRequest clientRequest, ProxyConnection proxyConnection) {
        connections.put(clientRequest, proxyConnection);
        log.debug("Added connection: {} -> {}, total: {}",
                clientRequest.remoteAddress(),
                proxyConnection.getEndpoint().getAddress(),
                connections.size());
    }

    public void removeConnection(HttpServerRequest clientRequest) {
        ProxyConnection connection = connections.remove(clientRequest);
        if (connection != null) {
            log.debug("Removed connection: {}, duration: {}ms, total: {}",
                    clientRequest.remoteAddress(),
                    connection.getDuration(),
                    connections.size());
        }
    }

    public ProxyConnection getConnection(HttpServerRequest clientRequest) {
        return connections.get(clientRequest);
    }

    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Disconnect connections that don't match the new route table
     */
    public void disconnectInvalidConnections(RouteMatcher newRouteMatcher) {
        log.info("Checking {} connections for route validity", connections.size());

        int disconnected = 0;
        for (Map.Entry<HttpServerRequest, ProxyConnection> entry : connections.entrySet()) {
            HttpServerRequest clientRequest = entry.getKey();
            ProxyConnection connection = entry.getValue();
            Route route = connection.getRoute();

            // Check if route still exists
            boolean routeExists = newRouteMatcher.match(
                    clientRequest.getHeader("Host"),
                    clientRequest.path()
            ).isPresent();

            if (!routeExists) {
                // Send 503 and close connection
                HttpServerResponse response = clientRequest.response();
                if (!response.ended()) {
                    response.setStatusCode(503);
                    response.setStatusMessage("Service Unavailable - Route configuration changed");
                    response.end();
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
        for (Map.Entry<HttpServerRequest, ProxyConnection> entry : connections.entrySet()) {
            HttpServerRequest request = entry.getKey();
            HttpServerResponse response = request.response();
            if (!response.ended()) {
                try {
                    response.reset();
                } catch (Exception e) {
                    log.warn("Error closing connection", e);
                }
            }
        }
        connections.clear();
    }
}
