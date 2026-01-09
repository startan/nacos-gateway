package pans.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QPS rate limiter using sliding window
 */
public class QpsRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(QpsRateLimiter.class);

    private final int maxQps;
    private final long windowSizeMs;
    private final ConcurrentMap<Long, AtomicInteger> windows = new ConcurrentHashMap<>();

    public QpsRateLimiter(int maxQps) {
        this(maxQps, 1000); // 1 second window
    }

    public QpsRateLimiter(int maxQps, long windowSizeMs) {
        this.maxQps = maxQps;
        this.windowSizeMs = windowSizeMs;
    }

    @Override
    public boolean tryAcquire() {
        long window = System.currentTimeMillis() / windowSizeMs;
        AtomicInteger counter = windows.computeIfAbsent(window, k -> new AtomicInteger(0));

        int current = counter.incrementAndGet();

        // Clean up old windows
        cleanupOldWindows(window);

        if (current > maxQps) {
            counter.decrementAndGet();
            log.debug("QPS limit exceeded: {}/{}", current, maxQps);
            return false;
        }

        return true;
    }

    private void cleanupOldWindows(long currentWindow) {
        // Remove windows older than current - 1
        windows.entrySet().removeIf(entry -> entry.getKey() < currentWindow - 1);
    }

    public int getMaxQps() {
        return maxQps;
    }
}
