package nextf.nacos.gateway.config.reader;

import java.util.Properties;

/**
 * Nacos URL configuration
 * Parses `nacos://` protocol URL parameters
 *
 * New URL format: nacos://<dataId>[:<group>]?<query-parameters>
 * - dataId: required
 * - group: optional, uses : as separator, defaults to DEFAULT_GROUP
 * - All other parameters are passed through to Nacos Client
 */
public class NacosUrlConfig {

    private final String dataId;
    private final String group;
    private final Properties nacosProperties;

    /**
     * Constructor for NacosUrlConfig
     *
     * @param dataId The configuration data ID (required)
     * @param group The configuration group (optional, defaults to DEFAULT_GROUP)
     * @param nacosProperties All query parameters as Properties (passed to Nacos Client)
     */
    public NacosUrlConfig(String dataId, String group, Properties nacosProperties) {
        this.dataId = dataId;
        this.group = group != null ? group : "DEFAULT_GROUP";
        this.nacosProperties = nacosProperties != null ? nacosProperties : new Properties();
    }

    /**
     * Get the Nacos Properties for use with Nacos Client
     * All query parameters are directly passed through
     */
    public Properties getProperties() {
        return nacosProperties;
    }

    public String getDataId() {
        return dataId;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "NacosUrlConfig{" +
                "dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", properties=" + nacosProperties +
                '}';
    }
}
