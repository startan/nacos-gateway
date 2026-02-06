package nextf.nacos.gateway.logging;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Access log context with rich information
 */
public class AccessLogContext {

    private final Instant timestamp;
    private final String method;
    private final String uri;
    private final String queryString;
    private final String protocol;
    private final int status;
    private final long bytesSent;
    private final long durationMs;
    private final String clientIp;
    private final String backend;
    private final String endpoint;

    private final Map<String, String> requestHeaders;
    private final Map<String, String> responseHeaders;

    private AccessLogContext(Builder builder) {
        this.timestamp = builder.timestamp;
        this.method = builder.method;
        this.uri = builder.uri;
        this.queryString = builder.queryString;
        this.protocol = builder.protocol;
        this.status = builder.status;
        this.bytesSent = builder.bytesSent;
        this.durationMs = builder.durationMs;
        this.clientIp = builder.clientIp;
        this.backend = builder.backend;
        this.endpoint = builder.endpoint;
        this.requestHeaders = Collections.unmodifiableMap(builder.requestHeaders);
        this.responseHeaders = Collections.unmodifiableMap(builder.responseHeaders);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getStatus() {
        return status;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getDurationMs() {
        return durationMs;
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

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public String getRequestHeader(String name) {
        return requestHeaders.get(name);
    }

    public String getResponseHeader(String name) {
        return responseHeaders.get(name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private String method;
        private String uri;
        private String queryString = "";
        private String protocol = "HTTP/1.1";
        private int status;
        private long bytesSent = 0;
        private long durationMs = 0;
        private String clientIp;
        private String backend;
        private String endpoint;
        private Map<String, String> requestHeaders = new HashMap<>();
        private Map<String, String> responseHeaders = new HashMap<>();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder bytesSent(long bytesSent) {
            this.bytesSent = bytesSent;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder backend(String backend) {
            this.backend = backend;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder requestHeaders(Map<String, String> headers) {
            this.requestHeaders = new HashMap<>(headers);
            return this;
        }

        public Builder addRequestHeader(String name, String value) {
            this.requestHeaders.put(name, value);
            return this;
        }

        public Builder responseHeaders(Map<String, String> headers) {
            this.responseHeaders = new HashMap<>(headers);
            return this;
        }

        public Builder addResponseHeader(String name, String value) {
            this.responseHeaders.put(name, value);
            return this;
        }

        public AccessLogContext build() {
            return new AccessLogContext(this);
        }
    }
}
