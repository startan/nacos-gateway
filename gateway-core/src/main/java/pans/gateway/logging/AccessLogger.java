package pans.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.LoggingConfig;

import java.time.LocalDateTime;

/**
 * Access logger
 */
public class AccessLogger {

    private static final Logger log = LoggerFactory.getLogger(AccessLogger.class);

    private final boolean verbose;
    private final LogFormatter formatter;

    public AccessLogger(LoggingConfig config) {
        this.verbose = config.isVerbose();

        if ("json".equalsIgnoreCase(config.getFormat())) {
            this.formatter = new JsonLogFormatter();
        } else {
            this.formatter = new TextLogFormatter();
        }
    }

    public void logRequest(String method, String path, int status, long duration,
                          String clientIp, String backend, String endpoint) {
        if (!verbose) {
            return;
        }

        AccessLog accessLog = new AccessLog(
                LocalDateTime.now(),
                "INFO",
                method,
                path,
                status,
                duration,
                clientIp,
                backend,
                endpoint
        );

        String formatted = formatter.format(accessLog);
        log.info("{}", formatted);
    }

    public boolean isVerbose() {
        return verbose;
    }
}
