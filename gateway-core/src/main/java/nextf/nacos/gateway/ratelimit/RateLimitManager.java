package nextf.nacos.gateway.ratelimit;

import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.RateLimitConfig;
import nextf.nacos.gateway.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rate limit manager with three-tier limiting:
 * 1. Global limits (gateway level)
 * 2. Backend limits (backend service group level)
 * 3. Client limits (per-client, can be overridden by backend config)
 */
public class RateLimitManager {

    private static final Logger log = LoggerFactory.getLogger(RateLimitManager.class);

    // Global limiters
    private final AtomicReference<QpsRateLimiter> globalQpsLimiter;
    private final AtomicReference<ConnectionRateLimiter> globalConnectionLimiter;

    // Backend-level limiters
    private final Map<String, BackendRateLimiter> backendLimiters = new ConcurrentHashMap<>();

    // Client-level limiters (identified by client IP)
    private final Map<String, ClientRateLimiter> clientLimiters = new ConcurrentHashMap<>();

    // Backend client rate limit configurations (for overriding server defaults)
    private final Map<String, RateLimitConfig> backendRateLimitConfigs = new ConcurrentHashMap<>();

    // Server-level configuration
    private final AtomicReference<RateLimitConfig> serverRateLimitConfig;

    public RateLimitManager(GatewayConfig config) {
        // Get server-level rate limit config (from server.rateLimit section)
        RateLimitConfig serverConfig = config.getServer() != null
                ? config.getServer().getRateLimit()
                : null;

        if (serverConfig == null) {
            // Use default values if not configured
            serverConfig = new RateLimitConfig();
        }

        // Use AtomicReference to support hot reload
        this.serverRateLimitConfig = new AtomicReference<>(serverConfig);
        this.globalQpsLimiter = new AtomicReference<>(new QpsRateLimiter(serverConfig.getMaxQps()));
        this.globalConnectionLimiter = new AtomicReference<>(new ConnectionRateLimiter(serverConfig.getMaxConnections()));

        log.info("Rate limit initialized: global QPS={}, global connections={}, per-client QPS={}, per-client connections={}",
                serverConfig.getMaxQps() == -1 ? "unlimited" : serverConfig.getMaxQps(),
                serverConfig.getMaxConnections() == -1 ? "unlimited" : serverConfig.getMaxConnections(),
                serverConfig.getMaxQpsPerClient() == -1 ? "unlimited" : serverConfig.getMaxQpsPerClient(),
                serverConfig.getMaxConnectionsPerClient() == -1 ? "unlimited" : serverConfig.getMaxConnectionsPerClient());
    }

    /**
     * Try to acquire permits for a request (QPS only, no connection check)
     * Connection limit should be checked separately when creating a new ProxyConnection
     * @param backendName backend service name
     * @param clientIp client IP address
     * @return true if QPS permits are acquired, false otherwise
     */
    public boolean tryAcquire(String backendName, String clientIp) {
        // 1. Check global QPS limit (handles -1 as unlimited, 0 as rejected)
        if (!globalQpsLimiter.get().tryAcquire()) {
            log.warn("Global QPS limit exceeded or rejected");
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

        // 3. Check client QPS limits (respects cascading: Backend -> Server -> -1)
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
        // 1. Check global connection limit (handles -1 as unlimited, 0 as rejected)
        if (!globalConnectionLimiter.get().tryAcquire()) {
            log.warn("Global connection limit exceeded or rejected");
            return false;
        }

        // 2. Check backend connection limits (if configured)
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            if (!backendLimiter.tryAcquireConnection()) {
                log.warn("Backend-level connection limit exceeded for: {}", backendName);
                globalConnectionLimiter.get().release();
                return false;
            }
        }

        // 3. Check client connection limits
        ClientRateLimiter clientLimiter = getOrCreateClientLimiter(clientIp, backendName);
        if (!clientLimiter.tryAcquireConnection()) {
            log.warn("Client-level connection limit exceeded for: {}", clientIp);
            globalConnectionLimiter.get().release();
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
        globalConnectionLimiter.get().release();

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
     * Cascading: Backend (!= -1) -> Server (!= -1) -> -1 (no limit)
     */
    private ClientRateLimiter getOrCreateClientLimiter(String clientIp, String backendName) {
        return clientLimiters.computeIfAbsent(clientIp, ip -> {
            RateLimitConfig serverConfig = serverRateLimitConfig.get();
            RateLimitConfig backendConfig = backendRateLimitConfigs.get(backendName);

            // Cascading: Backend (!= -1) -> Server (!= -1) -> -1 (no limit)
            int maxQps = (backendConfig != null && backendConfig.isQpsPerClientLimited())
                    ? backendConfig.getMaxQpsPerClient()
                    : (serverConfig.isQpsPerClientLimited()
                        ? serverConfig.getMaxQpsPerClient()
                        : -1);

            int maxConns = (backendConfig != null && backendConfig.isConnectionsPerClientLimited())
                    ? backendConfig.getMaxConnectionsPerClient()
                    : (serverConfig.isConnectionsPerClientLimited()
                        ? serverConfig.getMaxConnectionsPerClient()
                        : -1);

            log.debug("Creating client limiter for {}: QPS={}, Connections={}",
                    ip, maxQps == -1 ? "unlimited" : maxQps,
                    maxConns == -1 ? "unlimited" : maxConns);

            return new ClientRateLimiter(ip, maxQps, maxConns);
        });
    }

    /**
     * Add or update backend-level rate limiter
     */
    public void updateBackendLimiter(String backendName, BackendConfig backendConfig) {
        RateLimitConfig rateLimit = backendConfig.getRateLimit();
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
                    backendName,
                    rateLimit.getMaxQps() == -1 ? "unlimited" : rateLimit.getMaxQps(),
                    rateLimit.getMaxConnections() == -1 ? "unlimited" : rateLimit.getMaxConnections(),
                    rateLimit.getMaxQpsPerClient() == -1 ? "unlimited" : rateLimit.getMaxQpsPerClient(),
                    rateLimit.getMaxConnectionsPerClient() == -1 ? "unlimited" : rateLimit.getMaxConnectionsPerClient());
        } else {
            // Remove backend limiter if config is null
            backendLimiters.remove(backendName);
            backendRateLimitConfigs.remove(backendName);
            log.info("Removed backend rate limiter for {} (no limit configured)", backendName);
        }
    }

    /**
     * Update server-level rate limit configuration (hot reload support)
     * @param newConfig the new server rate limit configuration
     * @return true if update was successful, false otherwise
     */
    public boolean updateServerRateLimitConfig(RateLimitConfig newConfig) {
        if (newConfig == null) {
            log.warn("Attempted to update with null config, ignoring");
            return false;
        }

        RateLimitConfig oldConfig = serverRateLimitConfig.get();

        // Check if configuration actually changed
        if (configEquals(oldConfig, newConfig)) {
            log.debug("Server rate limit config unchanged, skipping update");
            return true;
        }

        try {
            // 1. Create new QPS limiter (accept counter reset)
            QpsRateLimiter newQpsLimiter = new QpsRateLimiter(newConfig.getMaxQps());

            // 2. Create new connection limiter (preserve current connection count)
            ConnectionRateLimiter oldConnLimiter = globalConnectionLimiter.get();
            int currentConns = oldConnLimiter.getCurrentConnections();
            ConnectionRateLimiter newConnLimiter = new ConnectionRateLimiter(newConfig.getMaxConnections());
            newConnLimiter.setCurrentConnections(currentConns);

            // 3. Check if connection limit is being tightened
            if (newConfig.getMaxConnections() < currentConns) {
                log.warn("New maxConnections ({}) is less than current connections ({}), " +
                        "existing connections will be allowed to decay naturally",
                        newConfig.getMaxConnections(), currentConns);
            }

            // 4. Atomic replacement
            serverRateLimitConfig.set(newConfig);
            globalQpsLimiter.set(newQpsLimiter);
            globalConnectionLimiter.set(newConnLimiter);

            log.info("Server rate limit config updated: QPS {} -> {}, Connections {} -> {}, " +
                            "Per-client QPS {} -> {}, Per-client connections {} -> {}",
                    oldConfig.getMaxQps() == -1 ? "unlimited" : oldConfig.getMaxQps(),
                    newConfig.getMaxQps() == -1 ? "unlimited" : newConfig.getMaxQps(),
                    oldConfig.getMaxConnections() == -1 ? "unlimited" : oldConfig.getMaxConnections(),
                    newConfig.getMaxConnections() == -1 ? "unlimited" : newConfig.getMaxConnections(),
                    oldConfig.getMaxQpsPerClient() == -1 ? "unlimited" : oldConfig.getMaxQpsPerClient(),
                    newConfig.getMaxQpsPerClient() == -1 ? "unlimited" : newConfig.getMaxQpsPerClient(),
                    oldConfig.getMaxConnectionsPerClient() == -1 ? "unlimited" : oldConfig.getMaxConnectionsPerClient(),
                    newConfig.getMaxConnectionsPerClient() == -1 ? "unlimited" : newConfig.getMaxConnectionsPerClient());

            return true;
        } catch (Exception e) {
            log.error("Failed to update server rate limit config", e);
            return false;
        }
    }

    /**
     * Compare two server rate limit configs for equality
     */
    private boolean configEquals(RateLimitConfig c1,
                                  RateLimitConfig c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        return c1.getMaxQps() == c2.getMaxQps() &&
                c1.getMaxConnections() == c2.getMaxConnections() &&
                c1.getMaxQpsPerClient() == c2.getMaxQpsPerClient() &&
                c1.getMaxConnectionsPerClient() == c2.getMaxConnectionsPerClient();
    }

    /**
     * Clear all existing client limiters
     * This can be called when server config is updated to force re-creation with new limits
     */
    public void clearClientLimiters() {
        int count = clientLimiters.size();
        clientLimiters.clear();
        log.info("Cleared {} client rate limiters", count);
    }
}
