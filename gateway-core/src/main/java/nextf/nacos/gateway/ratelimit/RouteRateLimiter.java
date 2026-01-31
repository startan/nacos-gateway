package nextf.nacos.gateway.ratelimit;

import nextf.nacos.gateway.config.RateLimitConfig;

/**
 * Route-level rate limiter
 * Tracks both QPS and connection limits for a route
 */
public class RouteRateLimiter {

    private final String routeId;
    private final int maxQps;
    private final int maxConnections;

    private final QpsRateLimiter qpsLimiter;
    private final ConnectionRateLimiter connectionLimiter;

    /**
     * Create a new route rate limiter
     * @param routeId the route identifier
     * @param config the rate limit configuration
     */
    public RouteRateLimiter(String routeId, RateLimitConfig config) {
        this.routeId = routeId;
        this.maxQps = config.getMaxQps();
        this.maxConnections = config.getMaxConnections();
        this.qpsLimiter = new QpsRateLimiter(config.getMaxQps());
        this.connectionLimiter = new ConnectionRateLimiter(config.getMaxConnections());
    }

    /**
     * Try to acquire permits for a request (QPS only, no connection check)
     * @return true if QPS permit is acquired, false otherwise
     */
    public boolean tryAcquireQps() {
        return qpsLimiter.tryAcquire();
    }

    /**
     * Try to acquire connection permit (called when creating a new connection)
     * @return true if connection permit is acquired, false otherwise
     */
    public boolean tryAcquireConnection() {
        return connectionLimiter.tryAcquire();
    }

    /**
     * Release connection permit (called when connection is closed)
     */
    public void releaseConnection() {
        connectionLimiter.release();
    }

    public String getRouteId() {
        return routeId;
    }

    public int getMaxQps() {
        return maxQps;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getCurrentConnections() {
        return connectionLimiter.getCurrentConnections();
    }

    @Override
    public String toString() {
        return "RouteRateLimiter{" +
                "routeId='" + routeId + '\'' +
                ", maxQps=" + maxQps +
                ", maxConnections=" + maxConnections +
                ", currentConnections=" + getCurrentConnections() +
                '}';
    }
}
