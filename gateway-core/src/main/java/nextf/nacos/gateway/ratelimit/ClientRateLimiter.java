package nextf.nacos.gateway.ratelimit;

/**
 * Per-client rate limiter
 * Tracks both QPS and connection limits for individual clients
 *
 * Value semantics:
 * - -1: no limit (unlimited)
 * - 0: reject all access
 * - > 0: normal limit
 */
public class ClientRateLimiter {

    private final String clientId;
    private final int maxQps;
    private final int maxConnections;

    private final boolean qpsLimited;
    private final boolean connectionLimited;

    private final QpsRateLimiter qpsLimiter;
    private final ConnectionRateLimiter connectionLimiter;

    /**
     * Create a new client rate limiter
     * @param clientId the client identifier (e.g., IP address)
     * @param maxQps maximum queries per second for this client (-1 for unlimited, 0 for reject all)
     * @param maxConnections maximum concurrent connections for this client (-1 for unlimited, 0 for reject all)
     */
    public ClientRateLimiter(String clientId, int maxQps, int maxConnections) {
        this.clientId = clientId;
        this.maxQps = maxQps;
        this.maxConnections = maxConnections;
        this.qpsLimited = maxQps != -1;
        this.connectionLimited = maxConnections != -1;
        this.qpsLimiter = new QpsRateLimiter(maxQps);
        this.connectionLimiter = new ConnectionRateLimiter(maxConnections);
    }

    /**
     * Try to acquire permits for a request (QPS only, no connection check)
     * @return true if QPS permit is acquired, false otherwise
     */
    public boolean tryAcquireQps() {
        return qpsLimited ? qpsLimiter.tryAcquire() : true;
    }

    /**
     * Try to acquire connection permit (called when creating a new connection)
     * @return true if connection permit is acquired, false otherwise
     */
    public boolean tryAcquireConnection() {
        return connectionLimited ? connectionLimiter.tryAcquire() : true;
    }

    /**
     * Release connection permit (called when connection is closed)
     */
    public void release() {
        if (connectionLimited) {
            connectionLimiter.release();
        }
    }

    public String getClientId() {
        return clientId;
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
        return "ClientRateLimiter{" +
                "clientId='" + clientId + '\'' +
                ", maxQps=" + maxQps +
                ", maxConnections=" + maxConnections +
                ", currentConnections=" + getCurrentConnections() +
                '}';
    }
}
