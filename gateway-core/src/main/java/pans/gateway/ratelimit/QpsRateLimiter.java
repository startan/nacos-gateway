package pans.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QPS rate limiter using sliding window
 */
public class QpsRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(QpsRateLimiter.class);

    private final int maxQps;
    private final static long WINDOW_SIZE_MS = 1_000L;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicLong counterWindow = new AtomicLong(0);

    public QpsRateLimiter(int maxQps) {
        this.maxQps = maxQps;
    }

    @Override
    public boolean tryAcquire() {
        do {
            long window = System.currentTimeMillis() / WINDOW_SIZE_MS;
            long counterWindowVal = counterWindow.get();
            if (counterWindowVal != window) {
                if (counterWindow.compareAndSet(counterWindowVal, window)) {
                    counter.set(0);
                } else {
                    continue;
                }
            }
            int current = counter.incrementAndGet();
            if (current > maxQps) {
                log.debug("QPS limit exceeded: {}/{}", current, maxQps);
                return false;
            }
            return true;
        } while (true);
    }

    public int getMaxQps() {
        return maxQps;
    }
}
