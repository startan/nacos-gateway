package nextf.nacos.gateway.config.reader;

import java.util.Properties;

/**
 * Nacos URL configuration
 * Parses `nacos://` protocol URL parameters
 */
public class NacosUrlConfig {

    private final String dataId;
    private final String group;
    private final String namespace;
    private final String serverAddr;
    private final String authMode;
    private final String accessKey;
    private final String secretKey;
    private final String username;
    private final String password;

    public NacosUrlConfig(String dataId, String group, String namespace, String serverAddr,
                          String authMode, String accessKey, String secretKey,
                          String username, String password) {
        this.dataId = dataId;
        this.group = group != null ? group : "DEFAULT_GROUP";
        this.namespace = namespace != null ? namespace : "";
        this.serverAddr = serverAddr;
        this.authMode = authMode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.username = username;
        this.password = password;
    }

    /**
     * Convert to Properties required by Nacos Client
     */
    public Properties getProperties() {
        Properties props = new Properties();
        props.put("serverAddr", serverAddr);
        props.put("namespace", namespace);

        // Set authentication information based on authentication mode
        if ("ak/sk".equalsIgnoreCase(authMode)) {
            if (accessKey != null && !accessKey.isEmpty()) {
                props.put("accessKey", accessKey);
            }
            if (secretKey != null && !secretKey.isEmpty()) {
                props.put("secretKey", secretKey);
            }
        } else if ("username/password".equalsIgnoreCase(authMode)) {
            if (username != null && !username.isEmpty()) {
                props.put("username", username);
            }
            if (password != null && !password.isEmpty()) {
                props.put("password", password);
            }
        }

        return props;
    }

    public String getDataId() {
        return dataId;
    }

    public String getGroup() {
        return group;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public String getAuthMode() {
        return authMode;
    }

    @Override
    public String toString() {
        return "NacosUrlConfig{" +
                "dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", namespace='" + namespace + '\'' +
                ", serverAddr='" + serverAddr + '\'' +
                ", authMode='" + authMode + '\'' +
                '}';
    }
}
