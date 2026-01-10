package pans.gateway.proxy;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpConnection;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.route.Route;

/**
 * Proxy connection representing a client-backend connection pair
 * Manages connection-level resources: HttpClient, Endpoint, Backend
 */
public class ProxyConnection {

    private final HttpConnection clientConnection;
    private final Route route;
    private final Endpoint endpoint;
    private final Backend backend;
    private final HttpClient httpClient;
    private final long createTime;

    public ProxyConnection(HttpConnection clientConnection, Route route, Endpoint endpoint,
                          Backend backend, HttpClient httpClient) {
        this.clientConnection = clientConnection;
        this.route = route;
        this.endpoint = endpoint;
        this.backend = backend;
        this.httpClient = httpClient;
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
