package nextf.nacos.gateway.config.reader;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Nacos URL解析器
 * 解析 nacos:// 协议的URL
 *
 * URL格式: nacos://<dataId>?group=<group>&namespace=<namespace>&serverAddr=<serverAddr>&auth-mode=<mode>&accessKey=<key>&secretKey=<secret>&username=<user>&password=<pass>
 */
public class NacosUrlParser {

    /**
     * 解析Nacos URL
     */
    public static NacosUrlConfig parse(String urlString) throws IOException {
        if (!urlString.startsWith("nacos://")) {
            throw new IOException("Invalid Nacos URL: must start with 'nacos://'");
        }

        // 移除协议前缀
        String withoutProtocol = urlString.substring(8); // "nacos://".length()

        // 分离dataId和查询参数
        String dataId;
        Map<String, String> params = new HashMap<>();

        int queryIndex = withoutProtocol.indexOf('?');
        if (queryIndex > 0) {
            dataId = withoutProtocol.substring(0, queryIndex);
            String queryString = withoutProtocol.substring(queryIndex + 1);
            params = parseQueryString(queryString);
        } else {
            dataId = withoutProtocol;
        }

        if (dataId.isEmpty()) {
            throw new IOException("Invalid Nacos URL: dataId is required");
        }

        // URL解码dataId
        dataId = URLDecoder.decode(dataId, StandardCharsets.UTF_8);

        // 提取必需和可选参数
        String serverAddr = params.get("serverAddr");
        if (serverAddr == null || serverAddr.isEmpty()) {
            throw new IOException("Invalid Nacos URL: serverAddr is required");
        }

        String group = params.get("group");
        String namespace = params.get("namespace");
        String authMode = params.get("auth-mode");
        String accessKey = params.get("accessKey");
        String secretKey = params.get("secretKey");
        String username = params.get("username");
        String password = params.get("password");

        return new NacosUrlConfig(dataId, group, namespace, serverAddr,
                                  authMode, accessKey, secretKey, username, password);
    }

    /**
     * 解析查询字符串
     */
    private static Map<String, String> parseQueryString(String queryString) throws IOException {
        Map<String, String> params = new HashMap<>();

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (kv.length == 1) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                params.put(key, "");
            }
        }

        return params;
    }
}
