package nextf.nacos.gateway.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for configuration variable replacement
 * Tests the complete flow from ConfigLoader to variable resolution
 */
@DisplayName("Configuration Variable Replacement Integration Tests")
class ConfigVariableIntegrationTest {

    private final ConfigLoader loader = new ConfigLoader();

    @BeforeEach
    void setUp() {
        // Set up test system properties
        System.setProperty("test.gateway.port.apiV1", "19999");
        System.setProperty("test.gateway.port.apiV2", "29999");
        System.setProperty("test.gateway.port.console", "17999");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("test.gateway.port.apiV1");
        System.clearProperty("test.gateway.port.apiV2");
        System.clearProperty("test.gateway.port.console");
    }

    @Test
    @DisplayName("Should load configuration with replaced variables from system properties")
    void testLoadConfigWithSystemPropertyVariables() throws Exception {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:test.gateway.port.apiV1}
                    apiV2: ${sys:test.gateway.port.apiV2}
                    apiConsole: ${sys:test.gateway.port.console}

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

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer().getPorts().getApiV1()).isEqualTo(19999);
        assertThat(config.getServer().getPorts().getApiV2()).isEqualTo(29999);
        assertThat(config.getServer().getPorts().getApiConsole()).isEqualTo(17999);
    }

    @Test
    @DisplayName("Should use default values when system properties not found")
    void testLoadConfigWithDefaultValues() throws Exception {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:unknown.port:-18848}
                    apiV2: ${sys:unknown.port2:-19848}
                    apiConsole: ${sys:unknown.port3:-18080}

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

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer().getPorts().getApiV1()).isEqualTo(18848);
        assertThat(config.getServer().getPorts().getApiV2()).isEqualTo(19848);
        assertThat(config.getServer().getPorts().getApiConsole()).isEqualTo(18080);
    }

    @Test
    @DisplayName("Should support mixed variables and hardcoded values")
    void testLoadConfigWithMixedVariablesAndHardcodedValues() throws Exception {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:test.gateway.port.apiV1}
                    apiV2: 19848
                    apiConsole: ${sys:test.gateway.port.console}

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

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer().getPorts().getApiV1()).isEqualTo(19999); // from variable
        assertThat(config.getServer().getPorts().getApiV2()).isEqualTo(19848); // hardcoded
        assertThat(config.getServer().getPorts().getApiConsole()).isEqualTo(17999); // from variable
    }

    @Test
    @DisplayName("Should support path concatenation with variables")
    void testLoadConfigWithPathConcatenation() throws Exception {
        // Arrange
        System.setProperty("test.log.path", "/var/log/gateway");
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

                accessLog:
                  enabled: true
                  output:
                    path: ${sys:test.log.path}/access.log
                """;

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getAccessLog()).isNotNull();
        assertThat(config.getAccessLog().getOutput().getPath()).isEqualTo("/var/log/gateway/access.log");

        // Cleanup
        System.clearProperty("test.log.path");
    }

    @Test
    @DisplayName("Should support shorthand variable syntax")
    void testLoadConfigWithShorthandVariableSyntax() throws Exception {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${test.gateway.port.apiV1}
                    apiV2: ${test.gateway.port.apiV2}
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

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer().getPorts().getApiV1()).isEqualTo(19999);
        assertThat(config.getServer().getPorts().getApiV2()).isEqualTo(29999);
        assertThat(config.getServer().getPorts().getApiConsole()).isEqualTo(18080);
    }

    @Test
    @DisplayName("Should load configuration without variables (backward compatibility)")
    void testLoadConfigWithoutVariables() throws Exception {
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
                """;

        // Act
        GatewayConfig config = loader.loadFromString(yaml);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getServer().getPorts().getApiV1()).isEqualTo(18848);
        assertThat(config.getServer().getPorts().getApiV2()).isEqualTo(19848);
        assertThat(config.getServer().getPorts().getApiConsole()).isEqualTo(18080);
    }
}
