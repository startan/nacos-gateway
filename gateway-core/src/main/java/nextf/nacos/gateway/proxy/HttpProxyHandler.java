package nextf.nacos.gateway.proxy;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import nextf.nacos.gateway.logging.AccessLogger;
import nextf.nacos.gateway.logging.AccessLogContext;
import nextf.nacos.gateway.model.Backend;
import nextf.nacos.gateway.model.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP/1 and HTTP/2 proxy handler
 */
public class HttpProxyHandler implements ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyHandler.class);

    private final HttpClient httpClient;
    private final String host;
    private final int port;
    private final AccessLogger accessLogger;
    private final Backend backend;
    private final Endpoint endpoint;

    /**
     * Simplified constructor - using ProxyConnection
     * @param proxyConnection object containing all connection-related information
     * @param accessLogger access logger
     */
    public HttpProxyHandler(ProxyConnection proxyConnection, AccessLogger accessLogger) {
        this.httpClient = proxyConnection.getHttpClient();
        this.host = proxyConnection.getEndpoint().getHost();
        this.port = proxyConnection.getBackendPort();
        this.accessLogger = accessLogger;
        this.backend = proxyConnection.getBackend();
        this.endpoint = proxyConnection.getEndpoint();
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

        // Record start time for access log
        long startTime = System.currentTimeMillis();
        String clientIp = request.remoteAddress().host();

        // Collect request headers for access log
        Map<String, String> requestHeaders = new HashMap<>();
        if (accessLogger != null && accessLogger.isEnabled()) {
            request.headers().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
        }

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
                    .onSuccess(proxyResponse -> handleProxyResponse(request, proxyResponse, response,
                            address, startTime, clientIp, requestHeaders))
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
                                     String address,
                                     long startTime,
                                     String clientIp,
                                     Map<String, String> requestHeaders) {
        // Set status code
        clientResponse.setStatusCode(proxyResponse.statusCode());
        clientResponse.setStatusMessage(proxyResponse.statusMessage());

        // Copy headers
        proxyResponse.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                clientResponse.putHeader(header.getKey(), header.getValue());
            }
        });

        // Enable chunked transfer encoding to correctly handle response body
        // This ensures proper response transmission when backend uses chunked encoding
        clientResponse.setChunked(true);

        // Collect response headers for access log
        Map<String, String> responseHeaders = new HashMap<>();
        if (accessLogger != null && accessLogger.isEnabled()) {
            proxyResponse.headers().forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
        }

        // Track bytes sent for access log
        final long[] bytesSent = {0};

        // Handle response body
        proxyResponse.handler(buffer -> {
            clientResponse.write(buffer);
            if (accessLogger != null && accessLogger.isEnabled()) {
                bytesSent[0] += buffer.length();
            }
        });

        proxyResponse.endHandler(v -> {
            clientResponse.end();
            log.debug("Response from {}: status {}", address, proxyResponse.statusCode());

            // Log access
            if (accessLogger != null && accessLogger.isEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                AccessLogContext context = AccessLogContext.builder()
                        .method(clientRequest.method().name())
                        .uri(clientRequest.path())
                        .queryString(clientRequest.query())
                        .protocol(clientRequest.version().toString())
                        .status(proxyResponse.statusCode())
                        .bytesSent(bytesSent[0])
                        .durationMs(duration)
                        .clientIp(clientIp)
                        .backend(backend != null ? backend.getName() : "")
                        .endpoint(endpoint != null ? endpoint.getAddress() : address)
                        .requestHeaders(requestHeaders)
                        .responseHeaders(responseHeaders)
                        .build();
                accessLogger.logAccess(context);
            }
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
