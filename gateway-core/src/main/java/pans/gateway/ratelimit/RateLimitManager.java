package pans.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.GatewayConfig;
import pans.gateway.config.RouteConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit manager
 */
public class RateLimitManager {

    private static final Logger log = LoggerFactory.getLogger(RateLimitManager.class);

    private final QpsRateLimiter globalQpsLimiter;
    private final ConnectionRateLimiter globalConnectionLimiter;
    private final Map<String, RouteRateLimiter> routeLimiters = new ConcurrentHashMap<>();

    public RateLimitManager(GatewayConfig config) {
        int globalQps = config.getRateLimit().getGlobalQpsLimit();
        int globalConnections = config.getRateLimit().getGlobalMaxConnections();

        this.globalQpsLimiter = new QpsRateLimiter(globalQps);
        this.globalConnectionLimiter = new ConnectionRateLimiter(globalConnections);

        log.info("Rate limit initialized: global QPS={}, global connections={}",
                globalQps, globalConnections);
    }

    /**
     * Try to acquire permit for a route
     * @param routeId route identifier
     * @return true if acquired, false otherwise
     */
    public boolean tryAcquire(String routeId) {
        // Check global QPS limit
        if (!globalQpsLimiter.tryAcquire()) {
            log.debug("Global QPS limit exceeded");
            return false;
        }

        // Check global connection limit
        if (!globalConnectionLimiter.tryAcquire()) {
            log.debug("Global connection limit exceeded");
            return false;
        }

        // Check route-level limits
        RouteRateLimiter routeLimiter = routeLimiters.get(routeId);
        if (routeLimiter != null) {
            if (!routeLimiter.tryAcquire()) {
                log.debug("Route-level limit exceeded for route: {}", routeId);
                // Release global connection permit
                globalConnectionLimiter.release();
                return false;
            }
        }

        return true;
    }

    /**
     * Release permit for a route
     * @param routeId route identifier
     */
    public void release(String routeId) {
        // Release global connection permit
        globalConnectionLimiter.release();

        // Release route-level connection permit
        RouteRateLimiter routeLimiter = routeLimiters.get(routeId);
        if (routeLimiter != null) {
            routeLimiter.release();
        }
    }

    /**
     * Add or update route-level rate limiter
     */
    public void updateRouteLimiter(String routeId, RouteConfig routeConfig, GatewayConfig globalConfig) {
        int qpsLimit = routeConfig.getQpsLimit() != null
                ? routeConfig.getQpsLimit()
                : globalConfig.getRateLimit().getDefaultQpsLimit();

        int connectionLimit = routeConfig.getMaxConnections() != null
                ? routeConfig.getMaxConnections()
                : globalConfig.getRateLimit().getDefaultMaxConnections();

        RouteRateLimiter limiter = new RouteRateLimiter(qpsLimit, connectionLimit);
        routeLimiters.put(routeId, limiter);

        log.debug("Updated rate limiter for route {}: QPS={}, connections={}",
                routeId, qpsLimit, connectionLimit);
    }

    /**
     * Remove route-level rate limiter
     */
    public void removeRouteLimiter(String routeId) {
        routeLimiters.remove(routeId);
    }

    /**
     * Get current global connection count
     */
    public int getCurrentGlobalConnections() {
        return globalConnectionLimiter.getCurrentConnections();
    }

    /**
     * Get max global connections
     */
    public int getMaxGlobalConnections() {
        return globalConnectionLimiter.getMaxConnections();
    }
}
