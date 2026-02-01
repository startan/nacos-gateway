package nextf.nacos.gateway.ratelimit;

import nextf.nacos.gateway.config.BackendConfig;
import nextf.nacos.gateway.config.GatewayConfig;
import nextf.nacos.gateway.config.RateLimitConfig;
import nextf.nacos.gateway.config.RouteConfig;
import nextf.nacos.gateway.proxy.ProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rate limit manager with four-tier limiting:
 * 1. Global limits (gateway level)
 * 2. Route limits (route level)
 * 3. Backend limits (backend service group level)
 * 4. Client limits (per-client, can be overridden by route/backend config)
 */
public class RateLimitManager {

    private static final Logger log = LoggerFactory.getLogger(RateLimitManager.class);

    // Global limiters
    private final AtomicReference<QpsRateLimiter> globalQpsLimiter;
    private final AtomicReference<ConnectionRateLimiter> globalConnectionLimiter;

    // Backend-level limiters
    private final Map<String, BackendRateLimiter> backendLimiters = new ConcurrentHashMap<>();

    // Route-level limiters
    private final Map<String, RouteRateLimiter> routeRateLimiters = new ConcurrentHashMap<>();

    // Client-level limiters (identified by client IP)
    private final Map<String, ClientRateLimiter> clientLimiters = new ConcurrentHashMap<>();

    // Backend client rate limit configurations (for overriding server defaults)
    private final Map<String, RateLimitConfig> backendRateLimitConfigs = new ConcurrentHashMap<>();

    // Route-level rate limit configurations (for overriding backend/server defaults)
    private final Map<String, RateLimitConfig> routeRateLimitConfigs = new ConcurrentHashMap<>();

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
     * @param routeId route identifier (Route.getId())
     * @return true if QPS permits are acquired, false otherwise
     */
    public boolean tryAcquire(String backendName, String clientIp, String routeId) {
        // 1. Check global QPS limit (handles -1 as unlimited, 0 as rejected)
        if (!globalQpsLimiter.get().tryAcquire()) {
            log.warn("Global QPS limit exceeded or rejected");
            return false;
        }

        // 2. Check route-level QPS limits
        if (routeId != null) {
            RouteRateLimiter routeLimiter = routeRateLimiters.get(routeId);
            if (routeLimiter != null) {
                if (!routeLimiter.tryAcquireQps()) {
                    log.warn("Route-level QPS limit exceeded for: {}", routeId);
                    return false;
                }
            }
        }

        // 3. Check backend QPS limits (if configured)
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            if (!backendLimiter.tryAcquireQps()) {
                log.warn("Backend-level QPS limit exceeded for: {}", backendName);
                return false;
            }
        }

        // 4. Check client QPS limits (respects cascading: Route -> Backend -> Server -> -1)
        ClientRateLimiter clientLimiter = getOrCreateClientLimiter(clientIp, backendName, routeId);
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
     * @param routeId route identifier (Route.getId())
     * @return true if connection permits are acquired, false otherwise
     */
    public boolean tryAcquireConnection(String backendName, String clientIp, String routeId) {
        // 1. Check global connection limit (handles -1 as unlimited, 0 as rejected)
        if (!globalConnectionLimiter.get().tryAcquire()) {
            log.warn("Global connection limit exceeded or rejected");
            return false;
        }

        // 2. Check route-level connection limits
        if (routeId != null) {
            RouteRateLimiter routeLimiter = routeRateLimiters.get(routeId);
            if (routeLimiter != null) {
                if (!routeLimiter.tryAcquireConnection()) {
                    log.warn("Route-level connection limit exceeded for: {}", routeId);
                    globalConnectionLimiter.get().release();
                    return false;
                }
            }
        }

        // 3. Check backend connection limits (if configured)
        BackendRateLimiter backendLimiter = backendLimiters.get(backendName);
        if (backendLimiter != null) {
            if (!backendLimiter.tryAcquireConnection()) {
                log.warn("Backend-level connection limit exceeded for: {}", backendName);
                globalConnectionLimiter.get().release();
                // Release route connection permit
                if (routeId != null) {
                    RouteRateLimiter routeLimiter = routeRateLimiters.get(routeId);
                    if (routeLimiter != null) {
                        routeLimiter.releaseConnection();
                    }
                }
                return false;
            }
        }

        // 4. Check client connection limits
        ClientRateLimiter clientLimiter = getOrCreateClientLimiter(clientIp, backendName, routeId);
        if (!clientLimiter.tryAcquireConnection()) {
            log.warn("Client-level connection limit exceeded for: {}", clientIp);
            globalConnectionLimiter.get().release();
            // Release route connection permit
            if (routeId != null) {
                RouteRateLimiter routeLimiter = routeRateLimiters.get(routeId);
                if (routeLimiter != null) {
                    routeLimiter.releaseConnection();
                }
            }
            if (backendLimiter != null) {
                backendLimiter.release();
            }
            return false;
        }

        return true;
    }

    /**
     * Release connection permits (called when ProxyConnection is closed)
     * @param proxyConnection the proxy connection being closed
     */
    public void releaseConnection(ProxyConnection proxyConnection) {
        if (proxyConnection == null) {
            log.warn("Attempted to release permits for null ProxyConnection");
            return;
        }

        String backendName = proxyConnection.getBackend().getName();
        String clientIp = proxyConnection.getClientIp();
        String routeId = proxyConnection.getRoute() != null ? proxyConnection.getRoute().getId() : null;

        // Release global connection permit
        globalConnectionLimiter.get().release();

        // Release route-level connection permit
        if (routeId != null) {
            RouteRateLimiter routeLimiter = routeRateLimiters.get(routeId);
            if (routeLimiter != null) {
                routeLimiter.releaseConnection();
            }
        }

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
     * Cascading: Route (!= -1) -> Backend (!= -1) -> Server (!= -1) -> -1 (no limit)
     */
    private ClientRateLimiter getOrCreateClientLimiter(String clientIp, String backendName, String routeId) {
        return clientLimiters.computeIfAbsent(clientIp, ip -> {
            RateLimitConfig serverConfig = serverRateLimitConfig.get();
            RateLimitConfig backendConfig = backendRateLimitConfigs.get(backendName);
            RateLimitConfig routeConfig = routeRateLimitConfigs.get(routeId);

            // Cascading: Route (!= -1) -> Backend (!= -1) -> Server (!= -1) -> -1 (no limit)

            // QPS limit
            int maxQps;
            if (routeConfig != null && routeConfig.isQpsPerClientLimited()) {
                maxQps = routeConfig.getMaxQpsPerClient();
            } else if (backendConfig != null && backendConfig.isQpsPerClientLimited()) {
                maxQps = backendConfig.getMaxQpsPerClient();
            } else if (serverConfig.isQpsPerClientLimited()) {
                maxQps = serverConfig.getMaxQpsPerClient();
            } else {
                maxQps = -1;
            }

            // Connections limit
            int maxConns;
            if (routeConfig != null && routeConfig.isConnectionsPerClientLimited()) {
                maxConns = routeConfig.getMaxConnectionsPerClient();
            } else if (backendConfig != null && backendConfig.isConnectionsPerClientLimited()) {
                maxConns = backendConfig.getMaxConnectionsPerClient();
            } else if (serverConfig.isConnectionsPerClientLimited()) {
                maxConns = serverConfig.getMaxConnectionsPerClient();
            } else {
                maxConns = -1;
            }

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
     * Add or update route-level rate limit configuration
     * @param routeId route identifier (Route.getId())
     * @param routeConfig route configuration containing rate limit settings
     */
    public void updateRouteLimiter(String routeId, RouteConfig routeConfig) {
        RateLimitConfig rateLimit = routeConfig.getRateLimit();
        if (rateLimit != null) {
            // Create RouteRateLimiter instance
            RouteRateLimiter limiter = new RouteRateLimiter(routeId, rateLimit);
            routeRateLimiters.put(routeId, limiter);

            // Store route rate limit config for client limiter creation
            routeRateLimitConfigs.put(routeId, rateLimit);

            // Clear client limiters that use this route
            clearClientLimiters();

            log.info("Created route rate limiter for {}: QPS={}, Connections={}, ClientQPS={}, ClientConns={}",
                    routeId,
                    rateLimit.getMaxQps() == -1 ? "unlimited" : rateLimit.getMaxQps(),
                    rateLimit.getMaxConnections() == -1 ? "unlimited" : rateLimit.getMaxConnections(),
                    rateLimit.getMaxQpsPerClient() == -1 ? "unlimited" : rateLimit.getMaxQpsPerClient(),
                    rateLimit.getMaxConnectionsPerClient() == -1 ? "unlimited" : rateLimit.getMaxConnectionsPerClient());
        } else {
            // Remove route limiter if config is null
            routeRateLimiters.remove(routeId);
            routeRateLimitConfigs.remove(routeId);
            log.info("Removed route rate limiter for {} (no limit configured)", routeId);
        }
    }

    /**
     * Update server-level rate limit configuration (hot reload support)
     * @param newConfig the new server rate limit configuration (null means reset to unlimited)
     * @return true if update was successful, false otherwise
     */
    public boolean updateServerRateLimitConfig(RateLimitConfig newConfig) {
        // If null config provided, reset to default (unlimited) values
        if (newConfig == null) {
            newConfig = new RateLimitConfig(); // All fields default to -1 (unlimited)
            log.info("Server rate limit config reset to unlimited (null config provided)");
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
     * Clear all existing route rate limit configurations
     * Called when routes are updated
     */
    public void clearRouteLimiters() {
        int count = routeRateLimiters.size();
        routeRateLimiters.clear();
        routeRateLimitConfigs.clear();
        log.info("Cleared {} route rate limiters", count);
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
