package pans.gateway.management;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check endpoint
 */
public class HealthEndpoint {

    private static final Logger log = LoggerFactory.getLogger(HealthEndpoint.class);

    private final String path;
    private volatile boolean healthy = true;

    public HealthEndpoint(String path) {
        this.path = path;
    }

    public void handle(HttpServerRequest request) {
        if (!request.path().equals(path)) {
            request.response().setStatusCode(404).end();
            return;
        }

        HttpServerResponse response = request.response();

        if (healthy) {
            JsonObject health = new JsonObject()
                    .put("status", "UP")
                    .put("timestamp", System.currentTimeMillis());

            response.putHeader("Content-Type", "application/json");
            response.setStatusCode(200);
            response.end(health.encode());
        } else {
            JsonObject health = new JsonObject()
                    .put("status", "DOWN")
                    .put("timestamp", System.currentTimeMillis());

            response.putHeader("Content-Type", "application/json");
            response.setStatusCode(503);
            response.end(health.encode());
        }
    }

    public String getPath() {
        return path;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public boolean matches(String path) {
        return this.path.equals(path);
    }
}
