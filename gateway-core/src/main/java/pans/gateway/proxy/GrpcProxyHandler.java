package pans.gateway.proxy;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.TimeoutConfig;

/**
 * gRPC proxy handler (HTTP/2 complete passthrough)
 */
public class GrpcProxyHandler implements ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(GrpcProxyHandler.class);
    private static final String GRPC_CONTENT_TYPE = "application/grpc";

    private final HttpClient httpClient;
    private final String host;
    private final int port;
    private final TimeoutConfig timeoutConfig;

    /**
     * Constructor with host and port
     */
    public GrpcProxyHandler(HttpClient httpClient, String host, int port, TimeoutConfig timeoutConfig) {
        this.httpClient = httpClient;
        this.host = host;
        this.port = port;
        this.timeoutConfig = timeoutConfig;
    }

    public static boolean isGrpcRequest(HttpServerRequest request) {
        String contentType = request.getHeader("content-type");
        return contentType != null && contentType.startsWith(GRPC_CONTENT_TYPE);
    }

    @Override
    public void handle(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        request.pause();

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
                    .onSuccess(proxyResponse -> handleGrpcResponse(request, proxyResponse, response))
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
                                     HttpServerResponse clientResponse) {
        log.debug("Received gRPC response from {}:{} status {}", host, port, proxyResponse.statusCode());

        // Copy all headers (complete passthrough)
        clientResponse.setStatusCode(proxyResponse.statusCode());
        clientResponse.setStatusMessage(proxyResponse.statusMessage());
        proxyResponse.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                clientResponse.putHeader(header.getKey(), header.getValue());
            }
        });

        // Handle backpressure for streaming
        proxyResponse.handler(buffer -> {
            if (clientResponse.writeQueueFull()) {
                proxyResponse.pause();
                clientResponse.drainHandler(v -> proxyResponse.resume());
            }
            clientResponse.write(buffer);
        });

        proxyResponse.endHandler(v -> {
            // Copy trailers from proxy response to client response
            proxyResponse.trailers().forEach(trailer -> {
                clientResponse.putTrailer(trailer.getKey(), trailer.getValue());
            });

            clientResponse.end();
            log.debug("gRPC response completed from {}:{}", host, port);
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
