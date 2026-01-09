package pans.gateway.ratelimit;

/**
 * Rate limiter interface
 */
public interface RateLimiter {

    /**
     * Try to acquire a permit
     * @return true if acquired, false otherwise
     */
    boolean tryAcquire();
}
