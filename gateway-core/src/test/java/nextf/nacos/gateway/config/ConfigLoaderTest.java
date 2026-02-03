package nextf.nacos.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static nextf.nacos.gateway.testutil.ConfigTestHelper.*;

/**
 * Unit tests for ConfigLoader
 * Tests configuration validation, YAML parsing, and error handling
 */
@DisplayName("ConfigLoader Tests")
class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    @DisplayName("Should load valid minimal configuration successfully")
    void testLoadValidMinimalConfig() throws IOException {
        // Arrange
        String yaml = createMinimalValidYaml();

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer()).isNotNull();
        assertThat(config.getRoutes()).hasSize(1);
        assertThat(config.getBackends()).hasSize(1);
    }

    @Test
    @DisplayName("Should load complete configuration with all fields")
    void testLoadCompleteConfig() throws IOException {
        // Arrange
        String yaml = createMultiBackendYaml();

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert - Server config
        ServerConfig server = config.getServer();
        assertThat(server).isNotNull();
        assertThat(server.getPorts()).isNotNull();
        assertThat(server.getPorts().getApiV1()).isEqualTo(18848);
        assertThat(server.getPorts().getApiV2()).isEqualTo(19848);
        assertThat(server.getPorts().getApiConsole()).isEqualTo(18080);

        // Assert - Rate limit config
        RateLimitConfig rateLimit = server.getRateLimit();
        assertThat(rateLimit).isNotNull();
        assertThat(rateLimit.getMaxQps()).isEqualTo(2000);
        assertThat(rateLimit.getMaxConnections()).isEqualTo(10000);
        assertThat(rateLimit.getMaxQpsPerClient()).isEqualTo(10);
        assertThat(rateLimit.getMaxConnectionsPerClient()).isEqualTo(5);

        // Assert - Routes
        List<RouteConfig> routes = config.getRoutes();
        assertThat(routes).hasSize(2);
        assertThat(routes.get(0).getHost()).isEqualTo("group1.nacos.io");
        assertThat(routes.get(0).getBackend()).isEqualTo("group1-service");
        assertThat(routes.get(1).getHost()).isEqualTo("group2.nacos.io");
        assertThat(routes.get(1).getBackend()).isEqualTo("group2-service");

        // Assert - Backends
        List<BackendConfig> backends = config.getBackends();
        assertThat(backends).hasSize(2);

        BackendConfig backend1 = backends.get(0);
        assertThat(backend1.getName()).isEqualTo("group1-service");
        assertThat(backend1.getLoadBalance()).isEqualTo("round-robin");
        assertThat(backend1.getPorts().getApiV1()).isEqualTo(8848);
        assertThat(backend1.getPorts().getApiV2()).isEqualTo(9848);
        assertThat(backend1.getPorts().getApiConsole()).isEqualTo(8080);
        assertThat(backend1.getEndpoints()).hasSize(2);
        assertThat(backend1.getEndpoints().get(0).getHost()).isEqualTo("10.0.1.1");
        assertThat(backend1.getEndpoints().get(0).getPriority()).isEqualTo(10);

        BackendConfig backend2 = backends.get(1);
        assertThat(backend2.getName()).isEqualTo("group2-service");
        assertThat(backend2.getLoadBalance()).isEqualTo("random");
        assertThat(backend2.getEndpoints()).hasSize(2);
        assertThat(backend2.getEndpoints().get(1).getHost()).isEqualTo("10.0.2.2");
        assertThat(backend2.getEndpoints().get(1).getPriority()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should load configuration with timeout settings")
    void testLoadConfigWithTimeout() throws IOException {
        // Arrange
        String yaml = createTimeoutYaml();

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        TimeoutConfig timeout = config.getTimeout();
        assertThat(timeout).isNotNull();
        assertThat(timeout.getConnectTimeoutSeconds()).isEqualTo(10);
        assertThat(timeout.getRequestTimeoutSeconds()).isEqualTo(30);
        assertThat(timeout.getIdleTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("Should load configuration with custom rate limits")
    void testLoadConfigWithCustomRateLimits() throws IOException {
        // Arrange
        String yaml = createRateLimitedYaml();

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert - Server rate limits
        RateLimitConfig serverRateLimit = config.getServer().getRateLimit();
        assertThat(serverRateLimit.getMaxQps()).isEqualTo(1000);
        assertThat(serverRateLimit.getMaxConnections()).isEqualTo(5000);
        assertThat(serverRateLimit.getMaxQpsPerClient()).isEqualTo(5);
        assertThat(serverRateLimit.getMaxConnectionsPerClient()).isEqualTo(2);

        // Assert - Backend rate limits
        RateLimitConfig backendRateLimit = config.getBackends().get(0).getRateLimit();
        assertThat(backendRateLimit.getMaxQps()).isEqualTo(500);
        assertThat(backendRateLimit.getMaxConnections()).isEqualTo(1000);
        assertThat(backendRateLimit.getMaxQpsPerClient()).isEqualTo(3);
        assertThat(backendRateLimit.getMaxConnectionsPerClient()).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty configuration")
    void testNullOrEmptyConfig(String configContent) {
        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(configContent))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Should reject configuration with missing server section")
    void testMissingServerConfig() {
        // Arrange
        String yaml = createMissingServerYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Server configuration is missing");
    }

    @Test
    @DisplayName("Should reject configuration with missing server ports")
    void testMissingServerPorts() {
        // Arrange
        String yaml = """
                server:
                  rateLimit:
                    maxQps: 2000

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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Server ports configuration is missing");
    }

    @Test
    @DisplayName("Should reject configuration with invalid port numbers")
    void testInvalidPortNumbers() {
        // Arrange
        String yaml = createInvalidPortYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Should reject configuration with zero port")
    void testZeroPort() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 0
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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Should reject configuration with negative port")
    void testNegativePort() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: -1
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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Should reject configuration with port exceeding 65535")
    void testPortTooLarge() {
        // Arrange
        String yaml = """
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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Should reject configuration with duplicate server ports")
    void testDuplicateServerPorts() {
        // Arrange
        String yaml = createDuplicatePortYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Duplicate port number");
    }

    @Test
    @DisplayName("Should reject configuration with missing routes")
    void testMissingRoutes() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No routes configured");
    }

    @Test
    @DisplayName("Should reject configuration with empty routes")
    void testEmptyRoutes() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes: []

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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No routes configured");
    }

    @Test
    @DisplayName("Should reject configuration with missing backends")
    void testMissingBackends() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No backends configured");
    }

    @Test
    @DisplayName("Should reject configuration with empty backends")
    void testEmptyBackends() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends: []
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No backends configured");
    }

    @Test
    @DisplayName("Should reject backend with missing name")
    void testBackendWithoutName() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: test-backend

                backends:
                  - ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Backend name is required");
    }

    @Test
    @DisplayName("Should reject backend with empty name")
    void testBackendWithEmptyName() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"
                    backend: ""

                backends:
                  - name: ""
                    ports:
                      apiV1: 8848
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Should reject backend with missing ports")
    void testBackendWithMissingPorts() {
        // Arrange
        String yaml = """
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
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("missing ports configuration");
    }

    @Test
    @DisplayName("Should reject backend with invalid port values")
    void testBackendWithInvalidPorts() {
        // Arrange
        String yaml = """
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
                      apiV1: 0
                      apiV2: 9848
                      apiConsole: 8080
                    loadBalance: round-robin
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid apiV1 port");
    }

    @Test
    @DisplayName("Should reject backend with no endpoints")
    void testBackendWithNoEndpoints() {
        // Arrange
        String yaml = """
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
                    endpoints: []
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("has no endpoints");
    }

    @Test
    @DisplayName("Should reject endpoint with missing host")
    void testEndpointWithMissingHost() {
        // Arrange
        String yaml = """
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
                      - priority: 10
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Endpoint host is required");
    }

    @Test
    @DisplayName("Should reject route referencing unknown backend")
    void testRouteWithUnknownBackend() {
        // Arrange
        String yaml = createUnknownBackendYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unknown backend");
    }

    @Test
    @DisplayName("Should reject route with missing host")
    void testRouteWithMissingHost() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - backend: test-backend

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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Route host is required");
    }

    @Test
    @DisplayName("Should reject route with missing backend")
    void testRouteWithMissingBackend() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080

                routes:
                  - host: "*.nacos.io"

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

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Route backend is required");
    }

    @Test
    @DisplayName("Should reject invalid load balance strategy")
    void testInvalidLoadBalanceStrategy() {
        // Arrange
        String yaml = createInvalidLoadBalanceYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid load balance strategy");
    }

    @Test
    @DisplayName("Should reject negative rate limit values")
    void testNegativeRateLimit() {
        // Arrange
        String yaml = createInvalidRateLimitYaml();

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("must be >= -1");
    }

    @Test
    @DisplayName("Should accept unlimited rate limit (-1)")
    void testUnlimitedRateLimit() throws IOException {
        // Arrange
        String yaml = createUnlimitedRateLimitYaml();

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        RateLimitConfig serverRateLimit = config.getServer().getRateLimit();
        assertThat(serverRateLimit.getMaxQps()).isEqualTo(-1);
        assertThat(serverRateLimit.getMaxConnections()).isEqualTo(-1);
        assertThat(serverRateLimit.getMaxQpsPerClient()).isEqualTo(-1);
        assertThat(serverRateLimit.getMaxConnectionsPerClient()).isEqualTo(-1);

        RateLimitConfig backendRateLimit = config.getBackends().get(0).getRateLimit();
        assertThat(backendRateLimit.getMaxQps()).isEqualTo(-1);
        assertThat(backendRateLimit.getMaxConnections()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should reject zero rate limit values (reject all)")
    void testZeroRateLimit() throws IOException {
        // Arrange - 0 is valid (means reject all)
        String yaml = """
                server:
                  ports:
                    apiV1: 18848
                    apiV2: 19848
                    apiConsole: 18080
                  rateLimit:
                    maxQps: 0
                    maxConnections: 0
                    maxQpsPerClient: 0
                    maxConnectionsPerClient: 0

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

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert - 0 should be accepted
        RateLimitConfig rateLimit = config.getServer().getRateLimit();
        assertThat(rateLimit.getMaxQps()).isZero();
        assertThat(rateLimit.getMaxConnections()).isZero();
    }

    @Test
    @DisplayName("Should reject invalid timeout values")
    void testInvalidTimeout() {
        // Arrange
        String yaml = """
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
                  connectTimeoutSeconds: 0
                  requestTimeoutSeconds: 30
                  idleTimeoutSeconds: 60
                """;

        // Act & Assert
        assertThatThrownBy(() -> loader.loadFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Connect timeout must be positive");
    }

    @Test
    @DisplayName("Should accept all valid load balance strategies")
    void testValidLoadBalanceStrategies() throws IOException {
        // Arrange
        String[] strategies = {"round-robin", "random", "least-connection"};

        for (String strategy : strategies) {
            String yaml = String.format("""
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
                        loadBalance: %s
                        endpoints:
                          - host: 10.0.0.1
                            priority: 10
                    """, strategy);

            // Act & Assert
            GatewayConfig config = loader.loadFromString(yaml);
            assertThat(config.getBackends().get(0).getLoadBalance()).isEqualTo(strategy);
        }
    }

    @Test
    @DisplayName("Should parse backend with default load balance strategy")
    void testDefaultLoadBalanceStrategy() throws IOException {
        // Arrange
        String yaml = """
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
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        assertThat(config.getBackends().get(0).getLoadBalance()).isEqualTo("round-robin");
    }

    @Test
    @DisplayName("Should parse endpoint with default priority")
    void testDefaultEndpointPriority() throws IOException {
        // Arrange
        String yaml = """
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
                """;

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        EndpointConfig endpoint = config.getBackends().get(0).getEndpoints().get(0);
        assertThat(endpoint.getPriority()).isEqualTo(10); // Default priority
    }

    @Test
    @DisplayName("Should parse backend ports with default values")
    void testDefaultBackendPorts() throws IOException {
        // Arrange
        String yaml = """
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
                    loadBalance: round-robin
                    ports: {}
                    endpoints:
                      - host: 10.0.0.1
                        priority: 10
                """;

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        BackendConfig.BackendPortsConfig ports = config.getBackends().get(0).getPorts();
        assertThat(ports.getApiV1()).isEqualTo(8848);
        assertThat(ports.getApiV2()).isEqualTo(9848);
        assertThat(ports.getApiConsole()).isEqualTo(8080);
    }
}
