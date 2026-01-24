package nextf.nacos.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection count rate limiter
 */
public class ConnectionRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ConnectionRateLimiter.class);

    private final int maxConnections;
    private final AtomicInteger currentConnections = new AtomicInteger(0);

    public ConnectionRateLimiter(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    @Override
    public boolean tryAcquire() {
        int current;
        do {
            current = currentConnections.get();
            if (current >= maxConnections) {
                log.debug("Connection limit exceeded: {}/{}", current, maxConnections);
                return false;
            }
        } while (!currentConnections.compareAndSet(current, current + 1));

        return true;
    }

    /**
     * Release a connection permit
     */
    public void release() {
        int newValue = currentConnections.decrementAndGet();
        if (newValue < 0) {
            // Should not happen, but handle gracefully
            currentConnections.incrementAndGet();
        }
    }

    /**
     * Get current connection count
     */
    public int getCurrentConnections() {
        return currentConnections.get();
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
