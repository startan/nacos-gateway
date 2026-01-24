package nextf.nacos.gateway.logging;

import java.time.LocalDateTime;

/**
 * Access log entry
 */
public class AccessLog {

    private final LocalDateTime timestamp;
    private final String level;
    private final String method;
    private final String path;
    private final int status;
    private final long duration;
    private final String clientIp;
    private final String backend;
    private final String endpoint;

    public AccessLog(LocalDateTime timestamp, String level, String method, String path,
                     int status, long duration, String clientIp, String backend, String endpoint) {
        this.timestamp = timestamp;
        this.level = level;
        this.method = method;
        this.path = path;
        this.status = status;
        this.duration = duration;
        this.clientIp = clientIp;
        this.backend = backend;
        this.endpoint = endpoint;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getLevel() {
        return level;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getBackend() {
        return backend;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
