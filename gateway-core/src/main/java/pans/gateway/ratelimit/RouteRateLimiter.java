package pans.gateway.ratelimit;

/**
 * Route-level rate limiter
 */
public class RouteRateLimiter {

    private final QpsRateLimiter qpsLimiter;
    private final ConnectionRateLimiter connectionLimiter;

    public RouteRateLimiter(int qpsLimit, int connectionLimit) {
        this.qpsLimiter = new QpsRateLimiter(qpsLimit);
        this.connectionLimiter = new ConnectionRateLimiter(connectionLimit);
    }

    public boolean tryAcquire() {
        if (!qpsLimiter.tryAcquire()) {
            return false;
        }
        if (!connectionLimiter.tryAcquire()) {
            return false;
        }
        return true;
    }

    public void release() {
        connectionLimiter.release();
    }

    public QpsRateLimiter getQpsLimiter() {
        return qpsLimiter;
    }

    public ConnectionRateLimiter getConnectionLimiter() {
        return connectionLimiter;
    }
}
