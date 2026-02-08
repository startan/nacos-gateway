package nextf.nacos.gateway.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ConfigVariableResolver
 * Tests variable resolution from environment variables and system properties
 */
@DisplayName("ConfigVariableResolver Tests")
class ConfigVariableResolverTest {

    private static final String TEST_ENV_VAR = "TEST_NACOS_GATEWAY_VAR";
    private static final String TEST_SYS_PROP = "test.nacos.gateway.prop";

    @BeforeEach
    void setUp() {
        // Set up test environment variable (if not already set)
        if (System.getenv(TEST_ENV_VAR) == null) {
            // Note: We can't actually set environment variables in Java
            // These tests will use System.setProperty as a fallback
        }
        // Set up test system property
        System.setProperty(TEST_SYS_PROP, "test-value-from-sys-prop");
    }

    @AfterEach
    void tearDown() {
        // Clean up system property
        System.clearProperty(TEST_SYS_PROP);
    }

    @Test
    @DisplayName("Should return null for null input")
    void testNullInput() {
        // Act & Assert
        assertThat(ConfigVariableResolver.resolve(null)).isNull();
    }

    @Test
    @DisplayName("Should return empty string for empty input")
    void testEmptyInput() {
        // Act & Assert
        assertThat(ConfigVariableResolver.resolve("")).isEmpty();
    }

    @Test
    @DisplayName("Should return unchanged content when no variables present")
    void testNoVariables() {
        // Arrange
        String content = "server:\n  ports:\n    apiV1: 18848";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo(content);
    }

    @Test
    @DisplayName("Should replace system property variable")
    void testSystemPropertyVariable() {
        // Arrange
        String content = "port: ${sys:" + TEST_SYS_PROP + "}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("port: test-value-from-sys-prop");
    }

    @Test
    @DisplayName("Should replace system property with default value when property exists")
    void testSystemPropertyWithDefaultFound() {
        // Arrange
        String content = "port: ${sys:" + TEST_SYS_PROP + ":-default}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("port: test-value-from-sys-prop");
    }

    @Test
    @DisplayName("Should use default value when system property not found")
    void testSystemPropertyWithDefaultNotFound() {
        // Arrange
        String content = "port: ${sys:unknown.property:-9090}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("port: 9090");
    }

    @Test
    @DisplayName("Should keep original when system property not found and no default")
    void testSystemPropertyNotFoundNoDefault() {
        // Arrange
        String content = "port: ${sys:unknown.property}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("port: ${sys:unknown.property}");
    }

    @Test
    @DisplayName("Should use system property in shorthand syntax")
    void testShorthandSyntaxSystemProperty() {
        // Arrange
        String content = "value: ${" + TEST_SYS_PROP + "}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("value: test-value-from-sys-prop");
    }

    @Test
    @DisplayName("Should use default value in shorthand syntax when property not found")
    void testShorthandSyntaxWithDefault() {
        // Arrange
        String content = "port: ${unknown.prop:-8080}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("port: 8080");
    }

    @Test
    @DisplayName("Should support path concatenation with variables")
    void testPathConcatenation() {
        // Arrange
        String content = "path: ${sys:" + TEST_SYS_PROP + "}/logs";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("path: test-value-from-sys-prop/logs");
    }

    @Test
    @DisplayName("Should support multiple variables in one line")
    void testMultipleVariables() {
        // Arrange
        String content = "a: ${sys:" + TEST_SYS_PROP + "}, b: ${sys:java.version}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).contains("test-value-from-sys-prop");
        assertThat(result).contains(System.getProperty("java.version"));
    }

    @Test
    @DisplayName("Should support nested variables")
    void testNestedVariables() {
        // Arrange
        String content = "path: ${outer:-${sys:" + TEST_SYS_PROP + "}}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("path: test-value-from-sys-prop");
    }

    @Test
    @DisplayName("Should handle complex nested variables with defaults")
    void testComplexNestedVariables() {
        // Arrange
        System.setProperty("var.b", "value-b");
        String content = "result: ${var.a:-${var.b:-default}}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("result: value-b");

        // Cleanup
        System.clearProperty("var.b");
    }

    @Test
    @DisplayName("Should use deepest default in nested variables")
    void testNestedVariablesWithAllDefaults() {
        // Arrange
        String content = "result: ${var.a:-${var.b:-default}}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("result: default");
    }

    @Test
    @DisplayName("Should handle variables in YAML configuration")
    void testVariablesInYamlConfig() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:unknown.port:-18848}
                    apiV2: ${sys:unknown.port2:-19848}
                    apiConsole: 18080
                """;

        // Act
        String result = ConfigVariableResolver.resolve(yaml);

        // Assert
        assertThat(result).contains("apiV1: 18848");
        assertThat(result).contains("apiV2: 19848");
        assertThat(result).contains("apiConsole: 18080");
    }

    @Test
    @DisplayName("Should handle variable in quoted string")
    void testVariableInQuotedString() {
        // Arrange
        String content = "pattern: \"${sys:" + TEST_SYS_PROP + "}/access.log\"";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("pattern: \"test-value-from-sys-prop/access.log\"");
    }

    @Test
    @DisplayName("Should handle multiple occurrences of same variable")
    void testMultipleOccurrencesOfSameVariable() {
        // Arrange
        String content = "a: ${sys:" + TEST_SYS_PROP + "}, b: ${sys:" + TEST_SYS_PROP + "}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("a: test-value-from-sys-prop, b: test-value-from-sys-prop");
    }

    @Test
    @DisplayName("Should handle variable with special characters in name")
    void testVariableWithSpecialCharacters() {
        // Arrange
        System.setProperty("test.special.var", "special-value");
        String content = "value: ${sys:test.special.var}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("value: special-value");

        // Cleanup
        System.clearProperty("test.special.var");
    }

    @Test
    @DisplayName("Should handle variable with dot in name")
    void testVariableWithDotInName() {
        // Arrange
        System.setProperty("my.var", "dot-value");
        String content = "value: ${sys:my.var}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("value: dot-value");

        // Cleanup
        System.clearProperty("my.var");
    }

    @Test
    @DisplayName("Should handle default value containing special characters")
    void testDefaultWithSpecialCharacters() {
        // Arrange
        String content = "path: ${sys:unknown:-/path/to/logs}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("path: /path/to/logs");
    }

    @Test
    @DisplayName("Should handle variable with colon in default value")
    void testVariableWithColonInDefault() {
        // Arrange
        String content = "url: ${sys:unknown:-http://localhost:8080}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("url: http://localhost:8080");
    }

    @Test
    @DisplayName("Should resolve user.home system property")
    void testUserHomeSystemProperty() {
        // Arrange
        String userHome = System.getProperty("user.home");
        String content = "home: ${sys:user.home}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("home: " + userHome);
    }

    @Test
    @DisplayName("Should resolve java.version system property")
    void testJavaVersionSystemProperty() {
        // Arrange
        String javaVersion = System.getProperty("java.version");
        String content = "version: ${sys:java.version}";

        // Act
        String result = ConfigVariableResolver.resolve(content);

        // Assert
        assertThat(result).isEqualTo("version: " + javaVersion);
    }

    @Test
    @DisplayName("Should handle complex configuration with multiple variables")
    void testComplexConfigurationWithMultipleVariables() {
        // Arrange
        System.setProperty("gateway.port", "19999");
        System.setProperty("gateway.log.path", "/var/log/gateway");

        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:gateway.port:-18848}
                    apiV2: 19848
                    apiConsole: 18080

                accessLog:
                  output:
                    path: ${sys:gateway.log.path:-./logs}/access.log
                  rotation:
                    fileNamePattern: "${sys:gateway.log.path:-./logs}/archive/access.%d{yyyy-MM-dd}.log"
                """;

        // Act
        String result = ConfigVariableResolver.resolve(yaml);

        // Assert
        assertThat(result).contains("apiV1: 19999");
        assertThat(result).contains("path: /var/log/gateway/access.log");
        assertThat(result).contains("/var/log/gateway/archive/access.%d{yyyy-MM-dd}.log");

        // Cleanup
        System.clearProperty("gateway.port");
        System.clearProperty("gateway.log.path");
    }

    @Test
    @DisplayName("Should preserve YAML structure after variable replacement")
    void testPreserveYamlStructure() {
        // Arrange
        String yaml = """
                server:
                  ports:
                    apiV1: ${sys:unknown.port:-18848}
                  rateLimit:
                    maxQps: 2000
                """;

        // Act
        String result = ConfigVariableResolver.resolve(yaml);

        // Assert
        assertThat(result).contains("server:");
        assertThat(result).contains("ports:");
        assertThat(result).contains("apiV1: 18848");
        assertThat(result).contains("rateLimit:");
        assertThat(result).contains("maxQps: 2000");
    }
}
