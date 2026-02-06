package nextf.nacos.gateway.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import nextf.nacos.gateway.config.AccessLogConfig;
import org.slf4j.LoggerFactory;

/**
 * Access logger - decoupled from logback.xml
 * Uses programmatic Logback configuration
 */
public class AccessLogger {

    private volatile AccessLogConfig config;
    private volatile Logger accessLogger;
    private final LoggerContext loggerContext;
    private volatile Appender<ILoggingEvent> appender;

    public AccessLogger(AccessLogConfig config) {
        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        reconfigure(config);
    }

    /**
     * Log access
     */
    public void logAccess(AccessLogContext context) {
        if (!config.isEnabled() || accessLogger == null) {
            return;
        }

        AccessLogEvent event = new AccessLogEvent(context);
        accessLogger.callAppenders(event);
    }

    /**
     * Reconfigure with new config (for hot reload)
     */
    public synchronized void reconfigure(AccessLogConfig newConfig) {
        // Stop old appender
        if (appender != null) {
            AccessLogAppenderFactory.stopAppender(appender);
        }

        this.config = newConfig;

        if (newConfig.isEnabled()) {
            // Create new appender
            this.appender = AccessLogAppenderFactory.createAppender(newConfig, loggerContext);

            // Create or get access logger
            Logger logger = loggerContext.exists("AccessLog");
            if (logger == null) {
                logger = loggerContext.getLogger("AccessLog");
            }
            logger.setAdditive(false); // Don't propagate to root logger
            logger.detachAndStopAllAppenders();
            logger.addAppender(this.appender);

            this.accessLogger = logger;
        } else {
            this.accessLogger = null;
            this.appender = null;
        }
    }

    /**
     * Check if access logging is enabled
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Get current config
     */
    public AccessLogConfig getConfig() {
        return config;
    }

    /**
     * Stop the logger and release resources
     */
    public synchronized void stop() {
        if (appender != null) {
            AccessLogAppenderFactory.stopAppender(appender);
            appender = null;
        }
        accessLogger = null;
    }
}
