package nextf.nacos.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.ServerConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit manager with three-tier limiting:
 * 1. Global limits (gateway level)
 * 2. Backend limits (backend service group level)
 * 3. Client limits (per-client, can be overridden by backend config)
 */
public class RateLimitManager {

    private static final Logger log = LoggerFactory.getLogger(RateLimitManager.class);

    // Global limiters
    private final QpsRateLimiter globalQpsLimiter;
    private final ConnectionRateLimiter globalConnectionLimiter;

    // Backend-level limiters
    private final Map<String, BackendRateLimiter> backendLimiters = new ConcurrentHashMap<>();

    // Client-level limiters (identified by client IP)
    private final Map<String, ClientRateLimiter> clientLimiters = new ConcurrentHashMap<>();

    // Backend client rate limit configurations (for overriding server defaults)
    private final Map<String, BackendConfig.BackendRateLimitConfig> backendRateLimitConfigs = new ConcurrentHashMap<>();

    // Server-level configuration
    private final ServerConfig.ServerRateLimitConfig serverRateLimitConfig;

    public RateLimitManager(GatewayConfig config) {
        // Get server-level rate limit config (from server.rateLimit section)
        ServerConfig.ServerRateLimitConfig serverConfig = config.getServer() != null
                ? config.getServer().getRateLimit()
                : null;

        if (serverConfig == null) {
            // Use default values if not configured
            this.serverRateLimitConfig = new ServerConfig.ServerRateLimitConfig();
        } else {
            this.serverRateLimitConfig = serverConfig;
        }

        this.globalQpsLimiter = new QpsRateLimiter(this.serverRateLimitConfig.getMaxQps());
        this.globalConnectionLimiter = new ConnectionRateLimiter(this.serverRateLimitConfig.getMaxConnections());

        log.info("Rate limit initialized: global QPS={}, global connections={}, per-client QPS={}, per-client connections={}",
                this.serverRateLimitConfig.getMaxQps(),
                this.serverRateLimitConfig.getMaxConnections(),
                this.serverRateLimitConfig.getMaxQpsPerClient(),
                this.serverRateLimitConfig.getMaxConnectionsPerClient());
    }

    /**
     * Try to acquire permits for a request (QPS only, no connection check)
     * Connection limit should be checked separately when creating a new ProxyConnection
     * @param backendName backend service name
     * @param clientIp client IP address
     * @return true if QPS permits are acquired, false otherwise
     */
    public boolean tryAcquire(String backendName, String clientIp) {
        // 1. Check global QPS limit
        if (!globalQpsLimiter.tryAcquire()) {
            log.warn("Global QPS limit exceeded");
            return false;
        }

        // 2. Check backend QPS limits (if configured)
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            if (!backendLimiter.tryAcquireQps()) {
                log.warn("Backend-level QPS limit exceeded for: {}", backendName);
                return false;
            }
        }

        // 3. Check client QPS limits
        ClientRateLimiter clientLimiter = getOrCreateClientLimiter(clientIp, backendName);
        if (!clientLimiter.tryAcquireQps()) {
            log.warn("Client-level QPS limit exceeded for: {}", clientIp);
            return false;
        }

        return true;
    }

    /**
     * Try to acquire connection permits (called when creating a new ProxyConnection)
     * @param backendName backend service name
     * @param clientIp client IP address
     * @return true if connection permits are acquired, false otherwise
     */
    public boolean tryAcquireConnection(String backendName, String clientIp) {
        // 1. Check global connection limit
        if (!globalConnectionLimiter.tryAcquire()) {
            log.warn("Global connection limit exceeded");
            return false;
        }

        // 2. Check backend connection limits (if configured)
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            if (!backendLimiter.tryAcquireConnection()) {
                log.warn("Backend-level connection limit exceeded for: {}", backendName);
                globalConnectionLimiter.release();
                return false;
            }
        }

        // 3. Check client connection limits
        ClientRateLimiter clientLimiter = getOrCreateClientLimiter(clientIp, backendName);
        if (!clientLimiter.tryAcquireConnection()) {
            log.warn("Client-level connection limit exceeded for: {}", clientIp);
            globalConnectionLimiter.release();
            if (backendLimiter != null) {
                backendLimiter.release();
            }
            return false;
        }

        return true;
    }

    /**
     * Release connection permits (called when ProxyConnection is closed)
     * @param backendName backend service name
     * @param clientIp client IP address
     */
    public void releaseConnection(String backendName, String clientIp) {
        // Release global connection permit
        globalConnectionLimiter.release();

        // Release backend connection permit
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            backendLimiter.release();
        }

        // Release client connection permit and cleanup if no active connections
        ClientRateLimiter clientLimiter = clientLimiters.get(clientIp);
        if (clientLimiter != null) {
            clientLimiter.release();

            // Clean up client limiter if all connections are closed
            if (clientLimiter.getCurrentConnections() == 0) {
                clientLimiters.remove(clientIp);
                log.debug("Removed client rate limiter for {} (no active connections)", clientIp);
            }
        }
    }

    /**
     * Get or create client rate limiter with proper config resolution
     * Backend config can override server defaults for client limits
     */
    private ClientRateLimiter getOrCreateClientLimiter(String clientIp, String backendName) {
        return clientLimiters.computeIfAbsent(clientIp, ip -> {
            // Start with server-level defaults
            int maxQps = serverRateLimitConfig.getMaxQpsPerClient();
            int maxConns = serverRateLimitConfig.getMaxConnectionsPerClient();

            // Backend config overrides server config (if configured)
            BackendConfig.BackendRateLimitConfig backendRateLimit = backendRateLimitConfigs.get(backendName);
            if (backendRateLimit != null) {
                // Only override if backend has configured these values (non-zero)
                if (backendRateLimit.getMaxQpsPerClient() > 0) {
                    maxQps = backendRateLimit.getMaxQpsPerClient();
                }
                if (backendRateLimit.getMaxConnectionsPerClient() > 0) {
                    maxConns = backendRateLimit.getMaxConnectionsPerClient();
                }
                log.debug("Using backend override for client {}: QPS={}, Connections={}",
                        ip, maxQps, maxConns);
            }

            return new ClientRateLimiter(ip, maxQps, maxConns);
        });
    }

    /**
     * Add or update backend-level rate limiter
     */
    public void updateBackendLimiter(String backendName, BackendConfig backendConfig) {
        BackendConfig.BackendRateLimitConfig rateLimit = backendConfig.getRateLimit();
        if (rateLimit != null) {
            // Update backend-level rate limiter
            BackendRateLimiter limiter = new BackendRateLimiter(
                    backendName,
                    rateLimit.getMaxQps(),
                    rateLimit.getMaxConnections()
            );
            backendLimiters.put(backendName, limiter);

            // Store backend rate limit config for client limiter creation
            backendRateLimitConfigs.put(backendName, rateLimit);

            log.info("Updated backend rate limiter for {}: QPS={}, Connections={}, ClientQPS={}, ClientConns={}",
                    backendName, rateLimit.getMaxQps(), rateLimit.getMaxConnections(),
                    rateLimit.getMaxQpsPerClient(), rateLimit.getMaxConnectionsPerClient());
        }
    }
}
