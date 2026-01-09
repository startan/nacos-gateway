package pans.gateway.proxy;

import io.vertx.core.http.HttpServerRequest;
import pans.gateway.model.Endpoint;
import pans.gateway.route.Route;

/**
 * Proxy connection representing a client-backend connection pair
 */
public class ProxyConnection {

    private final HttpServerRequest clientRequest;
    private final Route route;
    private final Endpoint endpoint;
    private final long createTime;

    public ProxyConnection(HttpServerRequest clientRequest, Route route, Endpoint endpoint) {
        this.clientRequest = clientRequest;
        this.route = route;
        this.endpoint = endpoint;
        this.createTime = System.currentTimeMillis();
    }

    public HttpServerRequest getClientRequest() {
        return clientRequest;
    }

    public Route getRoute() {
        return route;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - createTime;
    }
}
