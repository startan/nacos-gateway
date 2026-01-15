package pans.gateway.health;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.HealthProbeConfig;
import pans.gateway.model.Endpoint;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Health check task for a single endpoint
 * Supports both HTTP and TCP health checks
 */
public class HealthCheckTask {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckTask.class);

    private final Vertx vertx;
    private final Endpoint endpoint;
    private final HealthProbeConfig config;
    private final AtomicInteger consecutiveSuccesses;
    private final AtomicInteger consecutiveFailures;
    private final TcpHealthChecker tcpHealthChecker;
    private long timerId;

    public HealthCheckTask(Vertx vertx, Endpoint endpoint, HealthProbeConfig config) {
        this.vertx = vertx;
        this.endpoint = endpoint;
        this.config = config;
        this.consecutiveSuccesses = new AtomicInteger(0);
        this.consecutiveFailures = new AtomicInteger(0);
        this.tcpHealthChecker = new TcpHealthChecker(vertx, endpoint, config);
    }

    public void start() {
        log.info("Starting health check for endpoint: {}", endpoint.getAddress());

        long periodMs = config.getPeriodSeconds() * 1000L;
        timerId = vertx.setPeriodic(periodMs, this::checkHealth);
    }

    public void stop() {
        if (timerId != 0) {
            vertx.cancelTimer(timerId);
            timerId = 0;
            log.info("Stopped health check for endpoint: {}", endpoint.getAddress());
        }
    }

    private void checkHealth(long timerId) {
        // Check health check type
        if ("tcp".equalsIgnoreCase(config.getType())) {
            // Use TCP health check
            performTcpHealthCheck();
        } else {
            // Use HTTP health check (default)
            performHttpHealthCheck();
        }
    }

    /**
     * Perform TCP-based health check
     * Only checks if the port is reachable
     */
    private void performTcpHealthCheck() {
        tcpHealthChecker.check()
            .onSuccess(result -> {
                if (result) {
                    handleSuccess();
                } else {
                    handleFailure();
                }
            })
            .onFailure(t -> {
                log.warn("TCP health check failed for endpoint {}: {}", endpoint.getAddress(), t.getMessage());
                handleFailure();
            });
    }

    /**
     * Perform HTTP-based health check
     * Sends HTTP GET request to the health check path
     */
    private void performHttpHealthCheck() {
        HttpClient client = createHttpClient();
        int port = endpoint.getApiV1Port(); // Use apiV1 port for health checks

        client.request(HttpMethod.GET, port, endpoint.getHost(), config.getPath())
            .onSuccess(request -> {
                request.response()
                    .onSuccess(this::handleResponse)
                    .onFailure(t -> {
                        log.warn("HTTP health check failed for endpoint {}: {}", endpoint.getAddress(), t.getMessage());
                        handleFailure();
                    });

                request.exceptionHandler(t -> {
                    log.warn("HTTP health check failed for endpoint {}: {}", endpoint.getAddress(), t.getMessage());
                    handleFailure();
                });

                request.end();
            })
            .onFailure(t -> {
                log.warn("HTTP health check failed for endpoint {}: {}", endpoint.getAddress(), t.getMessage());
                handleFailure();
            });
    }

    private void handleResponse(HttpClientResponse response) {
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            handleSuccess();
        } else {
            log.warn("Health check failed for endpoint {}: HTTP {}", endpoint.getAddress(), statusCode);
            handleFailure();
        }
    }

    private void handleSuccess() {
        int successes = consecutiveSuccesses.incrementAndGet();
        consecutiveFailures.set(0);

        if (successes >= config.getSuccessThreshold() && !endpoint.isHealthy()) {
            endpoint.setHealthy(true); // Sync to registry
            log.info("Endpoint {} is now healthy", endpoint.getAddress());
        }
    }

    private void handleFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        consecutiveSuccesses.set(0);
        if (failures >= config.getFailureThreshold() && endpoint.isHealthy()) {
            endpoint.setHealthy(false); // Sync to registry
            log.warn("Endpoint {} is now unhealthy", endpoint.getAddress());
        }
    }

    private HttpClient createHttpClient() {
        return vertx.createHttpClient();
    }
}
