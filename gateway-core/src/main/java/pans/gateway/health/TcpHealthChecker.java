package pans.gateway.health;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import pans.gateway.config.HealthProbeConfig;
import pans.gateway.model.Endpoint;

/**
 * TCP-based health checker
 * Only checks if port is reachable
 */
public class TcpHealthChecker {

    private final Vertx vertx;
    private final Endpoint endpoint;
    private final HealthProbeConfig config;

    public TcpHealthChecker(Vertx vertx, Endpoint endpoint, HealthProbeConfig config) {
        this.vertx = vertx;
        this.endpoint = endpoint;
        this.config = config;
    }

    /**
     * Check if the endpoint's TCP port is reachable
     * @return Future&lt;Boolean&gt; true if port is reachable, false otherwise
     */
    public Future<Boolean> check() {
        return check(endpoint.getApiV1Port());
    }

    /**
     * Check if the specified TCP port is reachable
     * @param port the port to check
     * @return Future&lt;Boolean&gt; true if port is reachable, false otherwise
     */
    public Future<Boolean> check(int port) {
        NetClient client = vertx.createNetClient();
        return client.connect(port, endpoint.getHost())
            .map(socket -> {
                socket.close();
                client.close();
                return true;
            })
            .otherwise(false);
    }
}
