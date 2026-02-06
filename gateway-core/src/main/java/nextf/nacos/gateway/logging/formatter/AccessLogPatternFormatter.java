package nextf.nacos.gateway.logging.formatter;

import nextf.nacos.gateway.config.AccessLogConfig;
import nextf.nacos.gateway.logging.AccessLogContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Access log pattern formatter
 * Supports Tomcat-style access log patterns
 */
public class AccessLogPatternFormatter {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%\\{([^}]*)\\}([a-zA-Z])|%([a-zA-Z])");

    private final String pattern;
    private final DateTimeFormatter dateFormatter;

    public AccessLogPatternFormatter(String pattern) {
        this.pattern = pattern;
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
                .withZone(ZoneId.systemDefault());
    }

    public String format(AccessLogContext context) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = evaluatePlaceholder(matcher, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String evaluatePlaceholder(Matcher matcher, AccessLogContext context) {
        // Try %{name}type pattern first
        String name = matcher.group(1);
        String type = matcher.group(2);

        if (type != null) {
            return evaluateNamedPlaceholder(name, type, context);
        }

        // Try %type pattern
        type = matcher.group(3);
        return evaluateSimplePlaceholder(type, context);
    }

    private String evaluateNamedPlaceholder(String name, String type, AccessLogContext context) {
        return switch (type) {
            case "i" -> nullToEmpty(context.getRequestHeader(name)); // Request header
            case "o" -> nullToEmpty(context.getResponseHeader(name)); // Response header
            case "h" -> name.equalsIgnoreCase("clientIp") ? nullToEmpty(context.getClientIp()) : ""; // Client IP
            case "m" -> name.equalsIgnoreCase("method") ? nullToEmpty(context.getMethod()) : ""; // Method
            case "U" -> name.equalsIgnoreCase("uri") ? nullToEmpty(context.getUri()) : ""; // URI
            case "s" -> name.equalsIgnoreCase("status") ? String.valueOf(context.getStatus()) : ""; // Status
            case "b" -> name.equalsIgnoreCase("bytes") ? String.valueOf(context.getBytesSent()) : "-"; // Bytes
            case "D" -> name.equalsIgnoreCase("duration") ? String.valueOf(context.getDurationMs()) : ""; // Duration
            case "t" -> name.equalsIgnoreCase("timestamp") ? dateFormatter.format(context.getTimestamp()) : ""; // Timestamp
            default -> "";
        };
    }

    private String evaluateSimplePlaceholder(String type, AccessLogContext context) {
        return switch (type) {
            case "h" -> nullToEmpty(context.getClientIp()); // Client IP
            case "m" -> nullToEmpty(context.getMethod()); // Method
            case "U" -> nullToEmpty(context.getUri()); // URI
            case "s" -> String.valueOf(context.getStatus()); // Status
            case "b" -> context.getBytesSent() > 0 ? String.valueOf(context.getBytesSent()) : "-"; // Bytes
            case "D" -> String.valueOf(context.getDurationMs()); // Duration (ms)
            case "t" -> dateFormatter.format(context.getTimestamp()); // Timestamp
            case "H" -> nullToEmpty(context.getProtocol()); // Protocol
            case "r" -> nullToEmpty(context.getMethod()) + " " + nullToEmpty(context.getUri()) + " " + nullToEmpty(context.getProtocol()); // Request line
            case "u" -> "-"; // Remote user (not supported)
            case "T" -> String.format("%.3f", context.getDurationMs() / 1000.0); // Duration (seconds)
            case "n" -> "\n"; // Newline
            default -> "";
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
