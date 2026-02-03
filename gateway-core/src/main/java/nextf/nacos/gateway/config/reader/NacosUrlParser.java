package nextf.nacos.gateway.config.reader;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Nacos URL parser
 * Parses `nacos://` protocol URLs
 *
 * New URL format: nacos://<dataId>[:<group>]?<query-parameters>
 * - dataId: required
 * - group: optional, uses : as separator, defaults to DEFAULT_GROUP
 * - All query parameters are passed through to Nacos Client
 *
 * Examples:
 * - nacos://config.yaml:my-group?serverAddr=127.0.0.1:8848
 * - nacos://config.yaml?serverAddr=127.0.0.1:8848 (group defaults to DEFAULT_GROUP)
 * - nacos://gateway.yaml:prod?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=key&secretKey=secret
 */
public class NacosUrlParser {

    /**
     * Parse Nacos URL
     */
    public static NacosUrlConfig parse(String urlString) throws IOException {
        if (!urlString.startsWith("nacos://")) {
            throw new IOException("Invalid Nacos URL: must start with 'nacos://'");
        }

        // Remove protocol prefix
        String withoutProtocol = urlString.substring(8); // "nacos://".length()

        // Separate path and query parameters
        String pathPart;
        String queryString = "";

        int queryIndex = withoutProtocol.indexOf('?');
        if (queryIndex >= 0) {
            pathPart = withoutProtocol.substring(0, queryIndex);
            queryString = withoutProtocol.substring(queryIndex + 1);
        } else {
            pathPart = withoutProtocol;
        }

        // Separate dataId and group (group is optional, separated by :)
        String dataId;
        String group;

        int colonIndex = pathPart.indexOf(':');
        if (colonIndex >= 0) {
            // Has colon - separate dataId and group
            dataId = pathPart.substring(0, colonIndex);
            group = pathPart.substring(colonIndex + 1);
            // If group is empty, use default value
            if (group.isEmpty()) {
                group = "DEFAULT_GROUP";
            }
        } else {
            // No colon - entire path is dataId, use default group
            dataId = pathPart;
            group = "DEFAULT_GROUP";
        }

        // Validate dataId
        if (dataId.isEmpty()) {
            throw new IOException("Invalid Nacos URL: dataId is required");
        }

        // URL decode dataId and group
        dataId = URLDecoder.decode(dataId, StandardCharsets.UTF_8);
        group = URLDecoder.decode(group, StandardCharsets.UTF_8);

        // Parse query parameters to Properties
        Properties properties = parseQueryString(queryString);

        return new NacosUrlConfig(dataId, group, properties);
    }

    /**
     * Parse query string into Properties
     * All parameters are passed through to Nacos Client
     */
    private static Properties parseQueryString(String queryString) throws IOException {
        Properties properties = new Properties();

        if (queryString == null || queryString.isEmpty()) {
            return properties;
        }

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                properties.setProperty(key, value);
            } else if (kv.length == 1) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                properties.setProperty(key, "");
            }
        }

        return properties;
    }
}
