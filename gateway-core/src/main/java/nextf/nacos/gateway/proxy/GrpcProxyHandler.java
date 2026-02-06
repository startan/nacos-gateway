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
 * gRPC proxy handler (HTTP/2 complete passthrough)
 */
public class GrpcProxyHandler implements ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(GrpcProxyHandler.class);
    private static final String GRPC_CONTENT_TYPE = "application/grpc";

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
    public GrpcProxyHandler(ProxyConnection proxyConnection, AccessLogger accessLogger) {
        this.httpClient = proxyConnection.getHttpClient();
        this.host = proxyConnection.getEndpoint().getHost();
        this.port = proxyConnection.getBackendPort();
        this.accessLogger = accessLogger;
        this.backend = proxyConnection.getBackend();
        this.endpoint = proxyConnection.getEndpoint();
    }

    public static boolean isGrpcRequest(HttpServerRequest request) {
        String contentType = request.getHeader("content-type");
        return contentType != null && contentType.startsWith(GRPC_CONTENT_TYPE);
    }

    @Override
    public void handle(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        request.pause();

        // Record start time for access log
        long startTime = System.currentTimeMillis();
        String clientIp = request.remoteAddress().host();

        // Collect request headers for access log
        Map<String, String> requestHeaders = new HashMap<>();
        if (accessLogger != null && accessLogger.isEnabled()) {
            request.headers().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
        }

        // Create HTTP/2 proxy request
        httpClient.request(
                request.method(),
                port,
                host,
                request.uri())
            .onSuccess(proxyRequest -> {
                // Copy all headers (complete passthrough)
                copyHeaders(request, proxyRequest);
                proxyRequest.setChunked(true);

                // Handle backpressure
                request.handler(buffer -> {
                    if (proxyRequest.writeQueueFull()) {
                        request.pause();
                        proxyRequest.drainHandler(v -> request.resume());
                    }
                    proxyRequest.write(buffer);
                });

                request.endHandler(v -> {
                    proxyRequest.end();
                    log.debug("gRPC request proxied to {}:{}", host, port);
                });

                // Handle proxy response
                proxyRequest.response()
                    .onSuccess(proxyResponse -> handleGrpcResponse(request, proxyResponse, response,
                            startTime, clientIp, requestHeaders))
                    .onFailure(t -> {
                        log.error("Response from gRPC backend {}:{} failed: {}", host, port, t.getMessage());
                        if (!response.ended()) {
                            response.reset();
                        }
                    });

                request.resume();
            })
            .onFailure(t -> {
                log.error("Request to gRPC backend {}:{} failed: {}", host, port, t.getMessage());
                if (!response.ended()) {
                    response.reset();
                }
                request.resume();
            });
    }

    private void handleGrpcResponse(HttpServerRequest clientRequest,
                                     HttpClientResponse proxyResponse,
                                     HttpServerResponse clientResponse,
                                     long startTime,
                                     String clientIp,
                                     Map<String, String> requestHeaders) {
        log.debug("Received gRPC response from {}:{} status {}", host, port, proxyResponse.statusCode());

        // Copy all headers (complete passthrough)
        clientResponse.setStatusCode(proxyResponse.statusCode());
        clientResponse.setStatusMessage(proxyResponse.statusMessage());
        proxyResponse.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                clientResponse.putHeader(header.getKey(), header.getValue());
            }
        });

        // Collect response headers for access log
        Map<String, String> responseHeaders = new HashMap<>();
        if (accessLogger != null && accessLogger.isEnabled()) {
            proxyResponse.headers().forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
        }

        // Track bytes sent for access log
        final long[] bytesSent = {0};

        // Handle backpressure for streaming
        proxyResponse.handler(buffer -> {
            if (clientResponse.writeQueueFull()) {
                proxyResponse.pause();
                clientResponse.drainHandler(v -> proxyResponse.resume());
            }
            clientResponse.write(buffer);
            if (accessLogger != null && accessLogger.isEnabled()) {
                bytesSent[0] += buffer.length();
            }
        });

        proxyResponse.endHandler(v -> {
            try {
                // Only end if response hasn't been ended yet
                if (!clientResponse.ended()) {
                    // Copy trailers from proxy response to client response
                    proxyResponse.trailers().forEach(trailer -> {
                        clientResponse.putTrailer(trailer.getKey(), trailer.getValue());
                    });

                    clientResponse.end();
                    log.debug("gRPC response completed from {}:{}", host, port);

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
                                .endpoint(endpoint != null ? endpoint.getAddress() : host + ":" + port)
                                .requestHeaders(requestHeaders)
                                .responseHeaders(responseHeaders)
                                .build();
                        accessLogger.logAccess(context);
                    }
                } else {
                    log.debug("gRPC response already ended for {}:{}", host, port);
                }
            } catch (Exception e) {
                // Log warning but don't propagate - stream may already be closed
                log.warn("Failed to end client response for {}:{}: {}", host, port, e.getMessage());
            }
        });

        proxyResponse.exceptionHandler(t -> {
            log.error("Error reading gRPC response from {}:{} {}", host, port, t.getMessage());
            if (!clientResponse.ended()) {
                clientResponse.reset();
            }
        });
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
//                lower.equals("te") ||
//                lower.equals("trailers") ||
                lower.equals("transfer-encoding") ||
                lower.equals("upgrade");
    }
}
