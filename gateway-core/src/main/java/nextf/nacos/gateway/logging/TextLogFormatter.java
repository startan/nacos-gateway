package nextf.nacos.gateway.logging;

import java.time.format.DateTimeFormatter;

/**
 * Text log formatter
 */
public class TextLogFormatter implements LogFormatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(AccessLog log) {
        return String.format(
                "%s [%s] %s %s -> %s:%d [%d] %dms",
                log.getTimestamp().format(FORMATTER),
                log.getLevel(),
                log.getMethod(),
                log.getPath(),
                log.getBackend(),
                log.getEndpoint() != null ? extractPort(log.getEndpoint()) : 0,
                log.getStatus(),
                log.getDuration()
        );
    }

    private int extractPort(String endpoint) {
        try {
            String[] parts = endpoint.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
}
