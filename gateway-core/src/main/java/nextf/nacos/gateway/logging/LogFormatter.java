package nextf.nacos.gateway.logging;

/**
 * Log formatter interface
 */
public interface LogFormatter {

    /**
     * Format an access log entry
     */
    String format(AccessLog log);
}
