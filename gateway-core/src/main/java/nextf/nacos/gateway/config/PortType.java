package nextf.nacos.gateway.config;

/**
 * Port type enumeration for the gateway
 */
public enum PortType {
    API_V1("apiV1", "Nacos V1 API"),
    API_V2("apiV2", "Nacos V2 gRPC API"),
    API_CONSOLE("apiConsole", "Console API");

    private final String configName;
    private final String description;

    PortType(String configName, String description) {
        this.configName = configName;
        this.description = description;
    }

    public String getConfigName() {
        return configName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get PortType from config name
     * @param name the config name (e.g., "apiV1", "apiV2", "apiConsole")
     * @return the corresponding PortType
     * @throws IllegalArgumentException if the name is unknown
     */
    public static PortType fromConfigName(String name) {
        for (PortType type : values()) {
            if (type.configName.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown port type: " + name);
    }
}
