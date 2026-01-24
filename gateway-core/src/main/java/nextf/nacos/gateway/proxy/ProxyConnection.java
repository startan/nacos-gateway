package nextf.nacos.gateway.proxy;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpConnection;
import nextf.nacos.gateway.config.PortType;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.model.Endpoint;
import nextf.nacos.gateway.route.Route;

/**
 * Proxy connection representing a client-backend connection pair
 * Manages connection-level resources: HttpClient, Endpoint, Backend, PortType
 */
public class ProxyConnection {

    private final HttpConnection clientConnection;
    private final Route route;
    private final Endpoint endpoint;
    private final Backend backend;
    private final HttpClient httpClient;
    private final PortType portType;
    private final String clientIp;
    private final long createTime;

    /**
     * Constructor with port type
     * Used when creating a proxy connection for a specific port type
     */
    public ProxyConnection(HttpConnection clientConnection, Route route, Endpoint endpoint,
                          Backend backend, HttpClient httpClient, PortType portType, String clientIp) {
        this.clientConnection = clientConnection;
        this.route = route;
        this.endpoint = endpoint;
        this.backend = backend;
        this.httpClient = httpClient;
        this.portType = portType;
        this.clientIp = clientIp;
        this.createTime = System.currentTimeMillis();
    }

    public HttpConnection getClientConnection() {
        return clientConnection;
    }

    public Route getRoute() {
        return route;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Backend getBackend() {
        return backend;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public PortType getPortType() {
        return portType;
    }

    public String getClientIp() {
        return clientIp;
    }

    /**
     * Get the backend port for this connection's port type
     * @return the port number
     */
    public int getBackendPort() {
        return endpoint.getPortForType(portType);
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - createTime;
    }

    /**
     * Close all connection-level resources
     */
    public void close() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (backend != null && endpoint != null) {
            backend.getLoadBalancer().onConnectionClose(endpoint);
        }
    }
}
