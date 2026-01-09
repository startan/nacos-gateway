package pans.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration loader for loading and validating gateway configuration
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private final ObjectMapper yamlMapper;

    public ConfigLoader() {
        this.yamlMapper = createYamlMapper();
    }

    private ObjectMapper createYamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Load configuration from file path
     * First attempts to load from file system, then falls back to classpath
     */
    public GatewayConfig load(String configPath) throws IOException {
        log.info("Loading configuration from: {}", configPath);

        // Try file system first
        Path path = Paths.get(configPath);
        if (Files.exists(path)) {
            log.info("Loading configuration from file system: {}", path);
            return load(path);
        }

        // Fall back to classpath
        log.info("Configuration not found in file system, trying classpath: {}", configPath);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (inputStream == null) {
                throw new IOException("Configuration file not found in file system or classpath: " + configPath);
            }
            return load(inputStream);
        }
    }

    /**
     * Load configuration from path
     */
    public GatewayConfig load(Path configPath) throws IOException {
        log.info("Loading configuration from: {}", configPath);

        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        GatewayConfig config = yamlMapper.readValue(configPath.toFile(), GatewayConfig.class);

        // Validate configuration
        validate(config);

        log.info("Configuration loaded successfully: {}", config);
        return config;
    }

    /**
     * Load configuration from input stream
     */
    public GatewayConfig load(InputStream inputStream) throws IOException {
        GatewayConfig config = yamlMapper.readValue(inputStream, GatewayConfig.class);
        validate(config);
        return config;
    }

    /**
     * Load configuration from file
     */
    public GatewayConfig load(File file) throws IOException {
        GatewayConfig config = yamlMapper.readValue(file, GatewayConfig.class);
        validate(config);
        return config;
    }

    /**
     * Validate configuration
     */
    private void validate(GatewayConfig config) throws IOException {
        if (config == null) {
            throw new IOException("Configuration is null");
        }

        // Validate server config
        if (config.getServer() == null) {
            throw new IOException("Server configuration is missing");
        }
        if (config.getServer().getPort() <= 0 || config.getServer().getPort() > 65535) {
            throw new IOException("Invalid server port: " + config.getServer().getPort());
        }

        // Validate routes
        List<RouteConfig> routes = config.getRoutes();
        if (routes == null || routes.isEmpty()) {
            throw new IOException("No routes configured");
        }

        // Validate backends
        List<BackendConfig> backends = config.getBackends();
        if (backends == null || backends.isEmpty()) {
            throw new IOException("No backends configured");
        }

        // Collect backend names
        Set<String> backendNames = new HashSet<>();
        for (BackendConfig backend : backends) {
            if (backend.getName() == null || backend.getName().isEmpty()) {
                throw new IOException("Backend name is required");
            }
            backendNames.add(backend.getName());

            // Validate endpoints
            if (backend.getEndpoints() == null || backend.getEndpoints().isEmpty()) {
                throw new IOException("Backend '" + backend.getName() + "' has no endpoints");
            }

            for (EndpointConfig endpoint : backend.getEndpoints()) {
                if (endpoint.getHost() == null || endpoint.getHost().isEmpty()) {
                    throw new IOException("Endpoint host is required for backend '" + backend.getName() + "'");
                }
                if (endpoint.getPort() <= 0 || endpoint.getPort() > 65535) {
                    throw new IOException("Invalid port for endpoint '" + endpoint.getHost() + "' in backend '" + backend.getName() + "': " + endpoint.getPort());
                }
            }

            // Validate load balance strategy
            String loadBalance = backend.getLoadBalance();
            if (loadBalance == null || (!loadBalance.equals("round-robin") &&
                    !loadBalance.equals("random") &&
                    !loadBalance.equals("least-connection"))) {
                throw new IOException("Invalid load balance strategy for backend '" + backend.getName() + "': " + loadBalance);
            }
        }

        // Validate route references to backends
        for (RouteConfig route : routes) {
            if (route.getHost() == null || route.getHost().isEmpty()) {
                throw new IOException("Route host is required");
            }
            if (route.getPath() == null || route.getPath().isEmpty()) {
                throw new IOException("Route path is required");
            }
            if (route.getBackend() == null || route.getBackend().isEmpty()) {
                throw new IOException("Route backend is required for host: " + route.getHost());
            }
            if (!backendNames.contains(route.getBackend())) {
                throw new IOException("Route references unknown backend '" + route.getBackend() + "' for host: " + route.getHost());
            }
        }

        // Validate rate limit config
        RateLimitConfig rateLimit = config.getRateLimit();
        if (rateLimit != null) {
            if (rateLimit.getGlobalQpsLimit() <= 0) {
                throw new IOException("Global QPS limit must be positive");
            }
            if (rateLimit.getGlobalMaxConnections() <= 0) {
                throw new IOException("Global max connections must be positive");
            }
            if (rateLimit.getDefaultQpsLimit() <= 0) {
                throw new IOException("Default QPS limit must be positive");
            }
            if (rateLimit.getDefaultMaxConnections() <= 0) {
                throw new IOException("Default max connections must be positive");
            }
        }

        // Validate timeout config
        TimeoutConfig timeout = config.getTimeout();
        if (timeout != null) {
            if (timeout.getConnectTimeoutSeconds() <= 0) {
                throw new IOException("Connect timeout must be positive");
            }
            if (timeout.getRequestTimeoutSeconds() <= 0) {
                throw new IOException("Request timeout must be positive");
            }
            if (timeout.getIdleTimeoutSeconds() <= 0) {
                throw new IOException("Idle timeout must be positive");
            }
        }

        log.info("Configuration validation passed");
    }
}
