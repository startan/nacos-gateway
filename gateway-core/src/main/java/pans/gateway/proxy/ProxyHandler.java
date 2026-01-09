package pans.gateway.proxy;

import io.vertx.core.http.HttpServerRequest;

/**
 * Proxy handler interface
 */
public interface ProxyHandler {

    /**
     * Handle proxy request
     */
    void handle(HttpServerRequest request);
}
