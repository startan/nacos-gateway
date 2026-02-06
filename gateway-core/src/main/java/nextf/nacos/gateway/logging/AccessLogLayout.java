package nextf.nacos.gateway.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import nextf.nacos.gateway.config.AccessLogConfig;
import nextf.nacos.gateway.logging.formatter.AccessLogPatternFormatter;

/**
 * Access log layout for Logback
 * Formats AccessLog events into strings
 */
public class AccessLogLayout extends LayoutBase<ILoggingEvent> {

    private final AccessLogConfig config;
    private final AccessLogPatternFormatter patternFormatter;

    public AccessLogLayout(AccessLogConfig config) {
        this.config = config;
        this.patternFormatter = new AccessLogPatternFormatter(config.getPattern());
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        // Check if this is an AccessLogEvent
        if (event instanceof AccessLogEvent) {
            AccessLogEvent accessLogEvent = (AccessLogEvent) event;
            AccessLogContext context = accessLogEvent.getAccessLogContext();

            if ("json".equalsIgnoreCase(config.getFormat())) {
                return formatAsJson(context);
            } else {
                return patternFormatter.format(context);
            }
        }

        // Fallback for regular logging events
        return event.getFormattedMessage();
    }

    private String formatAsJson(AccessLogContext context) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":\"").append(context.getTimestamp()).append("\",");
        json.append("\"clientIp\":\"").append(escapeJson(context.getClientIp())).append("\",");
        json.append("\"method\":\"").append(escapeJson(context.getMethod())).append("\",");
        json.append("\"uri\":\"").append(escapeJson(context.getUri())).append("\",");
        json.append("\"protocol\":\"").append(escapeJson(context.getProtocol())).append("\",");
        json.append("\"status\":").append(context.getStatus()).append(",");
        json.append("\"bytesSent\":").append(context.getBytesSent()).append(",");
        json.append("\"durationMs\":").append(context.getDurationMs()).append(",");
        json.append("\"backend\":\"").append(escapeJson(context.getBackend())).append("\",");
        json.append("\"endpoint\":\"").append(escapeJson(context.getEndpoint())).append("\"");
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
