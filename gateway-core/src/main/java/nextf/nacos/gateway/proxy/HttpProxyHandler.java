package nextf.nacos.gateway.proxy;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.TimeoutConfig;

/**
 * HTTP/1 and HTTP/2 proxy handler
 */
public class HttpProxyHandler implements ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyHandler.class);

    private final HttpClient httpClient;
    private final String host;
    private final int port;
    private final TimeoutConfig timeoutConfig;

    /**
     * Constructor with host and port
     */
    public HttpProxyHandler(HttpClient httpClient, String host, int port, TimeoutConfig timeoutConfig) {
        this.httpClient = httpClient;
        this.host = host;
        this.port = port;
        this.timeoutConfig = timeoutConfig;
    }

    @Override
    public void handle(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        request.pause();

        String address = host + ":" + port;
        log.debug("Proxying {} {} to endpoint {}",
                request.method(),
                request.uri(),
                address);

        // Create proxy request
        httpClient.request(
                request.method(),
                port,
                host,
                request.uri())
            .onSuccess(proxyRequest -> {
                // Copy headers (skip hop-by-hop headers)
                copyHeaders(request, proxyRequest);
                proxyRequest.setChunked(true);

                // Handle request body with backpressure
                request.handler(buffer -> {
                    if (proxyRequest.writeQueueFull()) {
                        request.pause();
                        proxyRequest.drainHandler(v -> request.resume());
                    }
                    proxyRequest.write(buffer);
                });

                request.endHandler(v -> {
                    proxyRequest.end();
                    log.debug("Request proxied to {}", address);
                });

                // Handle proxy response
                proxyRequest.response()
                    .onSuccess(proxyResponse -> handleProxyResponse(request, proxyResponse, response, address))
                    .onFailure(t -> {
                        log.error("Response from backend {} failed: {}", address, t.getMessage());
                        if (!response.ended()) {
                            response.setStatusCode(502);
                            response.setStatusMessage("Bad Gateway");
                            response.end();
                        }
                    });

                request.resume();
            })
            .onFailure(t -> {
                log.error("Request to backend {} failed: {}", address, t.getMessage());
                if (!response.ended()) {
                    response.setStatusCode(502);
                    response.setStatusMessage("Bad Gateway");
                    response.end();
                }
                request.resume();
            });
    }

    private void handleProxyResponse(HttpServerRequest clientRequest,
                                     HttpClientResponse proxyResponse,
                                     HttpServerResponse clientResponse,
                                     String address) {
        // Set status code
        clientResponse.setStatusCode(proxyResponse.statusCode());
        clientResponse.setStatusMessage(proxyResponse.statusMessage());

        // Copy headers
        proxyResponse.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                clientResponse.putHeader(header.getKey(), header.getValue());
            }
        });

        // Handle response body
        proxyResponse.handler(clientResponse::write);

        proxyResponse.endHandler(v -> {
            clientResponse.end();
            log.debug("Response from {}: status {}", address, proxyResponse.statusCode());
        });

        proxyResponse.exceptionHandler(t -> {
            log.error("Error reading response from {}: {}", address, t.getMessage());
            if (!clientResponse.ended()) {
                clientResponse.reset();
            }
        });
    }

    private String buildTargetUrl(HttpServerRequest request) {
        return request.uri();
    }

    private void copyHeaders(HttpServerRequest from, HttpClientRequest to) {
        from.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                to.putHeader(header.getKey(), header.getValue());
            }
        });
    }

    private boolean isHopByHopHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("connection") ||
                lower.equals("keep-alive") ||
                lower.equals("proxy-authenticate") ||
                lower.equals("proxy-authorization") ||
                lower.equals("te") ||
                lower.equals("trailers") ||
                lower.equals("transfer-encoding") ||
                lower.equals("upgrade");
    }
}
