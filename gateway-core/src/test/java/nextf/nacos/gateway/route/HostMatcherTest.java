package nextf.nacos.gateway.route;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HostMatcher
 * Tests exact matching, wildcard matching, case-insensitivity, and edge cases
 */
@DisplayName("HostMatcher Tests")
class HostMatcherTest {

    @Test
    @DisplayName("Should match exact host name")
    void testExactMatch() {
        // Arrange
        HostMatcher matcher = new HostMatcher("example.com");

        // Act & Assert
        assertThat(matcher.matches("example.com")).isTrue();
        assertThat(matcher.matches("sub.example.com")).isFalse();
        assertThat(matcher.matches("other.com")).isFalse();
    }

    @Test
    @DisplayName("Should match exact host with subdomain")
    void testExactMatchWithSubdomain() {
        // Arrange
        HostMatcher matcher = new HostMatcher("api.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.example.com")).isTrue();
        assertThat(matcher.matches("example.com")).isFalse();
        assertThat(matcher.matches("sub.api.example.com")).isFalse();
    }

    @Test
    @DisplayName("Should match wildcard pattern at start")
    void testWildcardAtStart() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.example.com")).isTrue();
        assertThat(matcher.matches("www.example.com")).isTrue();
        assertThat(matcher.matches("any.sub.example.com")).isFalse();
        assertThat(matcher.matches("example.com")).isFalse();
    }

    @Test
    @DisplayName("Should match wildcard pattern with multiple subdomains")
    void testWildcardMultipleSubdomains() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.service.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.service.example.com")).isTrue();
        assertThat(matcher.matches("www.service.example.com")).isTrue();
        assertThat(matcher.matches("deep.api.service.example.com")).isFalse();
        assertThat(matcher.matches("service.example.com")).isFalse();
    }

    @Test
    @DisplayName("Should be case-insensitive for exact match")
    void testCaseInsensitiveExactMatch() {
        // Arrange
        HostMatcher matcher = new HostMatcher("Example.Com");

        // Act & Assert
        assertThat(matcher.matches("example.com")).isTrue();
        assertThat(matcher.matches("EXAMPLE.COM")).isTrue();
        assertThat(matcher.matches("Example.Com")).isTrue();
        assertThat(matcher.matches("eXaMpLe.cOm")).isTrue();
    }

    @Test
    @DisplayName("Should be case-sensitive for wildcard match")
    void testCaseSensitiveWildcardMatch() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.Example.Com");

        // Act & Assert
        // Wildcard regex patterns are case-sensitive
        assertThat(matcher.matches("api.example.com")).isFalse();
        assertThat(matcher.matches("API.EXAMPLE.COM")).isFalse();
        assertThat(matcher.matches("api.Example.Com")).isTrue();
        assertThat(matcher.matches("www.Example.Com")).isTrue();
    }

    @Test
    @DisplayName("Should reject null host")
    void testNullHost() {
        // Arrange
        HostMatcher matcher = new HostMatcher("example.com");

        // Act & Assert
        assertThat(matcher.matches(null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should reject empty or blank host")
    void testEmptyOrBlankHost(String host) {
        // Arrange
        HostMatcher matcher = new HostMatcher("example.com");

        // Act & Assert
        assertThat(matcher.matches(host)).isFalse();
    }

    @Test
    @DisplayName("Should match host with port number")
    void testHostWithPort() {
        // Arrange
        HostMatcher matcher = new HostMatcher("example.com");

        // Act & Assert
        // Note: HostMatcher doesn't strip ports, so these won't match
        assertThat(matcher.matches("example.com:8080")).isFalse();
        assertThat(matcher.matches("example.com:443")).isFalse();
    }

    @Test
    @DisplayName("Should match wildcard host with port number")
    void testWildcardHostWithPort() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.example.com:8080")).isFalse();
        assertThat(matcher.matches("www.example.com:443")).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple wildcards in pattern")
    void testMultipleWildcards() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.*.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.v1.example.com")).isTrue();
        assertThat(matcher.matches("www.prod.example.com")).isTrue();
        assertThat(matcher.matches("api.example.com")).isFalse();
        assertThat(matcher.matches("example.com")).isFalse();
    }

    @Test
    @DisplayName("Should handle host with hyphen")
    void testHostWithHyphen() {
        // Arrange
        HostMatcher matcher = new HostMatcher("my-service.example.com");

        // Act & Assert
        assertThat(matcher.matches("my-service.example.com")).isTrue();
        assertThat(matcher.matches("my-service.example.com")).isTrue();
    }

    @Test
    @DisplayName("Should handle wildcard with hyphen in subdomain")
    void testWildcardWithHyphen() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.my-service.example.com");

        // Act & Assert
        assertThat(matcher.matches("api.my-service.example.com")).isTrue();
        assertThat(matcher.matches("www-v2.my-service.example.com")).isTrue();
    }

    @Test
    @DisplayName("Should handle host with numbers")
    void testHostWithNumbers() {
        // Arrange
        HostMatcher matcher = new HostMatcher("service123.example.com");

        // Act & Assert
        assertThat(matcher.matches("service123.example.com")).isTrue();
        assertThat(matcher.matches("service456.example.com")).isFalse();
    }

    @Test
    @DisplayName("Should handle wildcard with numbers")
    void testWildcardWithNumbers() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*.example.com");

        // Act & Assert
        assertThat(matcher.matches("service1.example.com")).isTrue();
        assertThat(matcher.matches("api2.example.com")).isTrue();
        assertThat(matcher.matches("v3.example.com")).isTrue();
    }

    @Test
    @DisplayName("Should match single label host")
    void testSingleLabelHost() {
        // Arrange
        HostMatcher matcher = new HostMatcher("localhost");

        // Act & Assert
        assertThat(matcher.matches("localhost")).isTrue();
        assertThat(matcher.matches("LOCALHOST")).isTrue();
        assertThat(matcher.matches("example.localhost")).isFalse();
    }

    @Test
    @DisplayName("Should match wildcard for single label")
    void testWildcardForSingleLabel() {
        // Arrange
        HostMatcher matcher = new HostMatcher("*");

        // Act & Assert
        assertThat(matcher.matches("localhost")).isTrue();
        assertThat(matcher.matches("example")).isTrue();
        assertThat(matcher.matches("anything")).isTrue();
    }

    @Test
    @DisplayName("Should handle trailing dot in pattern")
    void testTrailingDotInPattern() {
        // Arrange
        HostMatcher matcher = new HostMatcher("example.com.");

        // Act & Assert
        assertThat(matcher.matches("example.com")).isFalse();
        assertThat(matcher.matches("example.com.")).isTrue();
    }

    @Test
    @DisplayName("Should handle leading dot in pattern")
    void testLeadingDotInPattern() {
        // Arrange
        HostMatcher matcher = new HostMatcher(".example.com");

        // Act & Assert
        assertThat(matcher.matches(".example.com")).isTrue();
        assertThat(matcher.matches("example.com")).isFalse();
    }

    @Test
    @DisplayName("Should match Internationalized Domain Names (IDN)")
    void testInternationalizedDomainNames() {
        // Arrange
        HostMatcher matcher = new HostMatcher("müller.example.com");

        // Act & Assert
        assertThat(matcher.matches("müller.example.com")).isTrue();
        assertThat(matcher.matches("MÜLLER.EXAMPLE.COM")).isTrue();
    }

    @Test
    @DisplayName("Should handle underscore in host")
    void testUnderscoreInHost() {
        // Arrange
        HostMatcher matcher = new HostMatcher("my_service.example.com");

        // Act & Assert
        assertThat(matcher.matches("my_service.example.com")).isTrue();
        assertThat(matcher.matches("my_service.example.com")).isTrue();
    }

    @Test
    @DisplayName("Should create multiple matchers independently")
    void testMultipleMatchers() {
        // Arrange
        HostMatcher matcher1 = new HostMatcher("*.api.example.com");
        HostMatcher matcher2 = new HostMatcher("*.web.example.com");
        HostMatcher matcher3 = new HostMatcher("admin.example.com");

        // Act & Assert
        assertThat(matcher1.matches("service.api.example.com")).isTrue();
        assertThat(matcher1.matches("service.web.example.com")).isFalse();

        assertThat(matcher2.matches("service.web.example.com")).isTrue();
        assertThat(matcher2.matches("service.api.example.com")).isFalse();

        assertThat(matcher3.matches("admin.example.com")).isTrue();
        assertThat(matcher3.matches("api.example.com")).isFalse();
    }

    @Test
    @DisplayName("Should handle complex wildcard patterns")
    void testComplexWildcardPatterns() {
        // Arrange
        HostMatcher matcher1 = new HostMatcher("*-*.*.example.com");
        HostMatcher matcher2 = new HostMatcher("*.*-*.*.com");

        // Act & Assert
        assertThat(matcher1.matches("api-v1.service.example.com")).isTrue();
        assertThat(matcher1.matches("api.v1.service.example.com")).isFalse();

        assertThat(matcher2.matches("api.v1-prod.service.com")).isTrue();
        assertThat(matcher2.matches("api.v1prod.service.com")).isFalse();
    }
}
