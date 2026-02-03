package nextf.nacos.gateway.testutil;

import nextf.nacos.gateway.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper methods for configuration testing
 */
public class ConfigTestHelper {

    /**
     * Create a minimal valid YAML configuration
     */
    public static String createMinimalValidYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: 2000
                    maxConnections: 10000
                    maxQpsPerClient: 10
                    maxConnectionsPerClient: 5

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML configuration with multiple backends
     */
    public static String createMultiBackendYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: 2000
                    maxConnections: 10000
                    maxQpsPerClient: 10
                    maxConnectionsPerClient: 5

                routes:
                  - host: "group1.nacos.io"
                    backend: group1-service
                  - host: "group2.nacos.io"
                    backend: group2-service

                backends:
                  - name: group1-service
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.1.1
                        priority: 10
                      - host: 10.0.1.2
                        priority: 10
                  - name: group2-service
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: random
                    endpoints:
                      - host: 10.0.2.1
                        priority: 10
                      - host: 10.0.2.2
                        priority: 20
                """;
    }

    /**
     * Create YAML configuration with custom rate limits
     */
    public static String createRateLimitedYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: 1000
                    maxConnections: 5000
                    maxQpsPerClient: 5
                    maxConnectionsPerClient: 2

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    rateLimit:
                      maxQps: 500
                      maxConnections: 1000
                      maxQpsPerClient: 3
                      maxConnectionsPerClient: 1
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML configuration with timeout settings
     */
    public static String createTimeoutYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10

                timeout:
                  connectTimeoutSeconds: 10
                  requestTimeoutSeconds: 30
                  idleTimeoutSeconds: 60
                """;
    }

    /**
     * Create YAML with invalid server config (missing)
     */
    public static String createMissingServerYaml() {
        return """
                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with invalid port numbers
     */
    public static String createInvalidPortYaml() {
        return """
                server:
                  ports:
                    apiV1: 70000
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with duplicate ports
     */
    public static String createDuplicatePortYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 18848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with unknown backend reference in route
     */
    public static String createUnknownBackendYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: unknown-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with invalid load balance strategy
     */
    public static String createInvalidLoadBalanceYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: invalid-strategy
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with invalid rate limit (negative values)
     */
    public static String createInvalidRateLimitYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: -10

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Create YAML with unlimited rate limits (-1 values)
     */
    public static String createUnlimitedRateLimitYaml() {
        return """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: -1
                    maxConnections: -1
                    maxQpsPerClient: -1
                    maxConnectionsPerClient: -1

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - name: test-backend
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    rateLimit:
                      maxQps: -1
                      maxConnections: -1
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;
    }

    /**
     * Write YAML content to a temporary file
     */
    public static Path writeTempFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("gateway-config-", ".yaml");
        Files.writeString(tempFile, content);
        return tempFile;
    }

    /**
     * Load configuration from YAML string using ConfigLoader
     */
    public static GatewayConfig loadFromYaml(String yaml) throws IOException {
        ConfigLoader loader = new ConfigLoader();
        return loader.loadFromString(yaml);
    }
}
