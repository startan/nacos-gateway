package pans.gateway.ratelimit;

/**
 * Backend-level rate limiter
 * Tracks both QPS and connection limits for a backend service group
 */
public class BackendRateLimiter {

    private final String backendName;
    private final int maxQps;
    private final int maxConnections;

    private final QpsRateLimiter qpsLimiter;
    private final ConnectionRateLimiter connectionLimiter;

    /**
     * Create a new backend rate limiter
     * @param backendName the backend service name
     * @param maxQps maximum queries per second for this backend
     * @param maxConnections maximum concurrent connections for this backend
     */
    public BackendRateLimiter(String backendName, int maxQps, int maxConnections) {
        this.backendName = backendName;
        this.maxQps = maxQps;
        this.maxConnections = maxConnections;
        this.qpsLimiter = new QpsRateLimiter(maxQps);
        this.connectionLimiter = new ConnectionRateLimiter(maxConnections);
    }

    /**
     * Try to acquire permits for a request
     * @return true if permits are acquired, false otherwise
     */
    public boolean tryAcquire() {
        if (!qpsLimiter.tryAcquire()) {
            return false;
        }
        if (!connectionLimiter.tryAcquire()) {
            return false;
        }
        return true;
    }

    /**
     * Release connection permit (called when connection is closed)
     */
    public void release() {
        connectionLimiter.release();
    }

    public String getBackendName() {
        return backendName;
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
        return "BackendRateLimiter{" +
                "backendName='" + backendName + '\'' +
                ", maxQps=" + maxQps +
                ", maxConnections=" + maxConnections +
                ", currentConnections=" + getCurrentConnections() +
                '}';
    }
}
