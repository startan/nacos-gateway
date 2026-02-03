package nextf.nacos.gateway.config.reader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NacosUrlParser
 * Tests URL parsing, parameter decoding, default values, and edge cases
 */
@DisplayName("NacosUrlParser Tests")
class NacosUrlParserTest {

    @Test
    @DisplayName("Should parse valid Nacos URL with all parameters using colon separator")
    void testParseValidUrlWithAllParams() throws IOException {
        // Arrange
        String url = "nacos://nacos-gateway.yaml:gateway-group?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=myKey&secretKey=mySecret";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getDataId()).isEqualTo("nacos-gateway.yaml");
        assertThat(config.getGroup()).isEqualTo("gateway-group");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("dev");
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
        assertThat(props.getProperty("accessKey")).isEqualTo("myKey");
        assertThat(props.getProperty("secretKey")).isEqualTo("mySecret");
    }

    @Test
    @DisplayName("Should parse Nacos URL with username/password authentication")
    void testParseUrlWithUsernamePassword() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:prod?namespace=production&serverAddr=192.168.1.100:8848&username=nacos&password=nacos123";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getDataId()).isEqualTo("config.yaml");
        assertThat(config.getGroup()).isEqualTo("prod");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("production");
        assertThat(props.getProperty("serverAddr")).isEqualTo("192.168.1.100:8848");
        assertThat(props.getProperty("username")).isEqualTo("nacos");
        assertThat(props.getProperty("password")).isEqualTo("nacos123");
    }

    @Test
    @DisplayName("Should parse Nacos URL without group (default to DEFAULT_GROUP)")
    void testParseUrlWithoutGroup() throws IOException {
        // Arrange
        String url = "nacos://gateway-config.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getDataId()).isEqualTo("gateway-config.yaml");
        assertThat(config.getGroup()).isEqualTo("DEFAULT_GROUP");
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("Should parse Nacos URL with empty group (default to DEFAULT_GROUP)")
    void testParseUrlWithEmptyGroup() throws IOException {
        // Arrange
        String url = "nacos://gateway-config.yaml:?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getDataId()).isEqualTo("gateway-config.yaml");
        assertThat(config.getGroup()).isEqualTo("DEFAULT_GROUP");
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("Should parse Nacos URL with group and namespace")
    void testParseUrlWithGroupAndNamespace() throws IOException {
        // Arrange
        String url = "nacos://test-config.yaml:test-group?namespace=test-ns&serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getDataId()).isEqualTo("test-config.yaml");
        assertThat(config.getGroup()).isEqualTo("test-group");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("test-ns");
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("Should parse Nacos URL with multiple server addresses")
    void testParseUrlWithMultipleServerAddresses() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=192.168.1.100:8848,192.168.1.101:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config).isNotNull();
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("192.168.1.100:8848,192.168.1.101:8848");
    }

    @Test
    @DisplayName("Should URL decode dataId")
    void testUrlDecodeDataId() throws IOException {
        // Arrange
        String url = "nacos://config%20file.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config file.yaml");
    }

    @Test
    @DisplayName("Should URL decode group")
    void testUrlDecodeGroup() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:test%20group?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getGroup()).isEqualTo("test group");
    }

    @Test
    @DisplayName("Should URL decode parameter values")
    void testUrlDecodeParameters() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:test%20group?namespace=test%2Fns&serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getGroup()).isEqualTo("test group");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("test/ns");
    }

    @Test
    @DisplayName("Should handle parameters without values")
    void testParametersWithoutValues() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:?serverAddr=127.0.0.1:8848&namespace=&group=";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getGroup()).isEqualTo("DEFAULT_GROUP");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("");
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("Should handle empty parameter values")
    void testEmptyParameterValues() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=127.0.0.1:8848&accessKey=&secretKey=";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("accessKey")).isEqualTo("");
        assertThat(props.getProperty("secretKey")).isEqualTo("");
    }

    @Test
    @DisplayName("Should parse URL with encoded slashes in dataId")
    void testEncodedSlashesInDataId() throws IOException {
        // Arrange
        String url = "nacos://path%2Fto%2Fconfig.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("path/to/config.yaml");
    }

    @Test
    @DisplayName("Should parse URL with encoded equals sign in parameter value")
    void testEncodedEqualsInParameter() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=127.0.0.1:8848&accessKey=key%3Dvalue";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
        assertThat(props.getProperty("accessKey")).isEqualTo("key=value");
    }

    @Test
    @DisplayName("Should parse URL with encoded colon in group")
    void testEncodedColonInGroup() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:group%3Asub?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getGroup()).isEqualTo("group:sub");
    }

    @Test
    @DisplayName("Should reject URL without nacos:// protocol")
    void testInvalidProtocol() {
        // Arrange
        String url = "file://config.yaml?serverAddr=127.0.0.1:8848";

        // Act & Assert
        assertThatThrownBy(() -> NacosUrlParser.parse(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("must start with 'nacos://'");
    }

    @Test
    @DisplayName("Should reject URL without protocol prefix")
    void testMissingProtocol() {
        // Arrange
        String url = "config.yaml?serverAddr=127.0.0.1:8848";

        // Act & Assert
        assertThatThrownBy(() -> NacosUrlParser.parse(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("must start with 'nacos://'");
    }

    @Test
    @DisplayName("Should reject null URL")
    void testNullUrl() {
        // Act & Assert
        assertThatThrownBy(() -> NacosUrlParser.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject URL with missing dataId")
    void testMissingDataId() {
        // Arrange
        String url = "nacos://?serverAddr=127.0.0.1:8848";

        // Act & Assert
        assertThatThrownBy(() -> NacosUrlParser.parse(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("dataId is required");
    }

    @Test
    @DisplayName("Should reject URL with only protocol prefix")
    void testOnlyProtocolPrefix() {
        // Arrange
        String url = "nacos://";

        // Act & Assert
        assertThatThrownBy(() -> NacosUrlParser.parse(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("dataId is required");
    }

    @Test
    @DisplayName("Should parse URL without query parameters")
    void testUrlWithoutQueryParameters() throws IOException {
        // Arrange
        String url = "nacos://config.yaml";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config.yaml");
        assertThat(config.getGroup()).isEqualTo("DEFAULT_GROUP");
        assertThat(config.getProperties().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should parse URL without query parameters but with group")
    void testUrlWithGroupNoQueryParameters() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:my-group";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config.yaml");
        assertThat(config.getGroup()).isEqualTo("my-group");
        assertThat(config.getProperties().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should parse URL with special characters in dataId")
    void testSpecialCharactersInDataId() throws IOException {
        // Arrange
        String url = "nacos://config_file-v1.0.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config_file-v1.0.yaml");
    }

    @Test
    @DisplayName("Should parse URL with dot in dataId")
    void testDotInDataId() throws IOException {
        // Arrange
        String url = "nacos://nacos-gateway.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("nacos-gateway.yaml");
    }

    @Test
    @DisplayName("Should parse URL with path in dataId")
    void testPathInDataId() throws IOException {
        // Arrange
        String url = "nacos://path/to/nacos-gateway.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("path/to/nacos-gateway.yaml");
    }

    @Test
    @DisplayName("Should parse URL with port in server address")
    void testPortInServerAddress() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
    }

    @Test
    @DisplayName("Should parse URL without port in server address")
    void testNoPortInServerAddress() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=127.0.0.1";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Should handle URL with many query parameters")
    void testManyQueryParameters() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:g1?namespace=ns1&serverAddr=127.0.0.1:8848&accessKey=key&secretKey=secret&username=user&password=pass&extra1=value1&extra2=value2";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config.yaml");
        assertThat(config.getGroup()).isEqualTo("g1");
        Properties props = config.getProperties();
        assertThat(props.getProperty("namespace")).isEqualTo("ns1");
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
        assertThat(props.getProperty("accessKey")).isEqualTo("key");
        assertThat(props.getProperty("secretKey")).isEqualTo("secret");
        assertThat(props.getProperty("username")).isEqualTo("user");
        assertThat(props.getProperty("password")).isEqualTo("pass");
        assertThat(props.getProperty("extra1")).isEqualTo("value1");
        assertThat(props.getProperty("extra2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should parse URL in different order")
    void testDifferentParameterOrder() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:group1?accessKey=key&serverAddr=127.0.0.1:8848&namespace=ns1";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config.yaml");
        assertThat(config.getGroup()).isEqualTo("group1");
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
        assertThat(props.getProperty("accessKey")).isEqualTo("key");
        assertThat(props.getProperty("namespace")).isEqualTo("ns1");
    }

    @Test
    @DisplayName("Should parse URL with IP address")
    void testIpAddressInServerAddr() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=192.168.1.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("192.168.1.1:8848");
    }

    @Test
    @DisplayName("Should parse URL with hostname")
    void testHostnameInServerAddr() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=nacos.example.com:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("nacos.example.com:8848");
    }

    @Test
    @DisplayName("Should parse URL with localhost")
    void testLocalhostInServerAddr() throws IOException {
        // Arrange
        String url = "nacos://config.yaml?serverAddr=localhost:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("serverAddr")).isEqualTo("localhost:8848");
    }

    @Test
    @DisplayName("Should handle whitespace in dataId after decoding")
    void testWhitespaceInDataId() throws IOException {
        // Arrange
        String url = "nacos://config%20file%20test.yaml?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getDataId()).isEqualTo("config file test.yaml");
    }

    @Test
    @DisplayName("Should handle whitespace in group after decoding")
    void testWhitespaceInGroup() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:my%20group?serverAddr=127.0.0.1:8848";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        assertThat(config.getGroup()).isEqualTo("my group");
    }

    @Test
    @DisplayName("Should verify all query parameters are in Properties")
    void testAllQueryParametersInProperties() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:my-group?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=key&secretKey=secret&username=user&password=pass&custom1=value1&custom2=value2";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.size()).isEqualTo(8);
        assertThat(props.getProperty("namespace")).isEqualTo("dev");
        assertThat(props.getProperty("serverAddr")).isEqualTo("127.0.0.1:8848");
        assertThat(props.getProperty("accessKey")).isEqualTo("key");
        assertThat(props.getProperty("secretKey")).isEqualTo("secret");
        assertThat(props.getProperty("username")).isEqualTo("user");
        assertThat(props.getProperty("password")).isEqualTo("pass");
        assertThat(props.getProperty("custom1")).isEqualTo("value1");
        assertThat(props.getProperty("custom2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should handle AK/SK authentication parameters")
    void testAKSKAuthentication() throws IOException {
        // Arrange
        String url = "nacos://gateway.yaml:group1?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=yourKey&secretKey=yourSecret";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("accessKey")).isEqualTo("yourKey");
        assertThat(props.getProperty("secretKey")).isEqualTo("yourSecret");
    }

    @Test
    @DisplayName("Should handle username/password authentication parameters")
    void testUsernamePasswordAuthentication() throws IOException {
        // Arrange
        String url = "nacos://config.yaml:prod?namespace=prod&serverAddr=192.168.1.100:8848&username=nacos&password=nacos";

        // Act
        NacosUrlConfig config = NacosUrlParser.parse(url);

        // Assert
        Properties props = config.getProperties();
        assertThat(props.getProperty("username")).isEqualTo("nacos");
        assertThat(props.getProperty("password")).isEqualTo("nacos");
    }
}
