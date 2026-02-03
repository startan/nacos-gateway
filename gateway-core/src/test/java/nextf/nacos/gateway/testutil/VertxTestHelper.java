package nextf.nacos.gateway.testutil;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Helper methods for Vert.x testing
 */
public class VertxTestHelper {

    /**
     * Create a test Vert.x instance
     */
    public static Vertx createVertx() {
        return Vertx.vertx();
    }

    /**
     * Create an HTTP server for testing
     */
    public static HttpServer createHttpServer(Vertx vertx, int port) {
        return vertx.createHttpServer(new HttpServerOptions().setPort(port));
    }

    /**
     * Create an HTTP client for testing
     */
    public static HttpClient createHttpClient(Vertx vertx) {
        HttpClientOptions options = new HttpClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(5);
        return vertx.createHttpClient(options);
    }

    /**
     * Create a TCP server for testing
     */
    public static NetServer createTcpServer(Vertx vertx, int port) {
        NetServerOptions options = new NetServerOptions().setPort(port);
        return vertx.createNetServer(options);
    }

    /**
     * Create a TCP client for testing
     */
    public static NetClient createTcpClient(Vertx vertx) {
        return vertx.createNetClient();
    }

    /**
     * Start HTTP server and wait for it to be ready
     * Note: Uses server.actualPort() after listening
     */
    public static int startServerSync(HttpServer server) throws Exception {
        var future = new CompletableFuture<Integer>();
        server.listen().onComplete(ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result().actualPort());
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return future.get(5, TimeUnit.SECONDS);
    }

    /**
     * Start TCP server and wait for it to be ready
     * Note: Uses server.actualPort() after listening
     */
    public static int startServerSync(NetServer server) throws Exception {
        var future = new CompletableFuture<Integer>();
        server.listen().onComplete(ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result().actualPort());
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return future.get(5, TimeUnit.SECONDS);
    }

    /**
     * Wait for async operation to complete with timeout
     */
    public static <T> T await(CompletableFuture<T> future, long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }

    /**
     * Wait for async operation to complete with default timeout (5 seconds)
     */
    public static <T> T await(CompletableFuture<T> future) throws Exception {
        return await(future, 5, TimeUnit.SECONDS);
    }

    /**
     * Find an available port for testing
     */
    public static int findAvailablePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find available port", e);
        }
    }

    /**
     * Check if a port is available
     */
    public static boolean isPortAvailable(int port) {
        try (var socket = new java.net.ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sleep for specified milliseconds (useful for testing timing-dependent code)
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Close Vert.x instance safely
     */
    public static void closeVertx(Vertx vertx) {
        if (vertx != null) {
            try {
                vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }

    /**
     * Close HTTP server safely
     */
    public static void closeServer(HttpServer server) {
        if (server != null) {
            try {
                server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }

    /**
     * Close TCP server safely
     */
    public static void closeServer(NetServer server) {
        if (server != null) {
            try {
                server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }

    /**
     * Runnable that will close resources automatically
     */
    public static class AutoCloseableVertx implements AutoCloseable {
        private final Vertx vertx;

        public AutoCloseableVertx() {
            this.vertx = createVertx();
        }

        public Vertx vertx() {
            return vertx;
        }

        @Override
        public void close() {
            closeVertx(vertx);
        }
    }

    /**
     * Runnable that will close HTTP server automatically
     */
    public static class AutoCloseableServer implements AutoCloseable {
        private final HttpServer server;

        public AutoCloseableServer(HttpServer server) {
            this.server = server;
        }

        public HttpServer server() {
            return server;
        }

        @Override
        public void close() {
            closeServer(server);
        }
    }
}
