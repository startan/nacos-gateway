package nextf.nacos.gateway.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import nextf.nacos.gateway.config.AccessLogAsyncConfig;
import nextf.nacos.gateway.config.AccessLogConfig;
import nextf.nacos.gateway.config.AccessLogRotationConfig;

/**
 * Factory for creating Logback appenders for access log
 */
public class AccessLogAppenderFactory {

    public static Appender<ILoggingEvent> createAppender(AccessLogConfig config, LoggerContext loggerContext) {
        // Create RollingFileAppender for ILoggingEvent
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(loggerContext);
        appender.setName("AccessLogAppender");

        // Configure file path and encoder
        appender.setFile(config.getOutput().getPath());

        // Create encoder with our custom layout
        AccessLogLayout layout = new AccessLogLayout(config);
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setLayout(layout);
        encoder.start();  // Critical fix: start the encoder
        appender.setEncoder(encoder);

        // Configure rolling policy
        configureRollingPolicy(appender, config.getRotation(), loggerContext);

        // Start appender
        appender.start();

        // Wrap with async appender if enabled
        if (config.getAsync().isEnabled()) {
            return wrapWithAsync(appender, config.getAsync(), loggerContext);
        }

        return appender;
    }

    private static void configureRollingPolicy(RollingFileAppender<ILoggingEvent> appender,
                                               AccessLogRotationConfig rotationConfig,
                                               LoggerContext loggerContext) {
        String policy = rotationConfig.getPolicy();

        if ("size".equalsIgnoreCase(policy) || "both".equalsIgnoreCase(policy)) {
            // Size and time based rolling
            SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setFileNamePattern(buildFileNamePattern(rotationConfig));
            rollingPolicy.setMaxFileSize(FileSize.valueOf(rotationConfig.getMaxFileSize()));
            rollingPolicy.setMaxHistory(rotationConfig.getMaxHistory());
            rollingPolicy.setParent(appender);
            rollingPolicy.start();
            appender.setRollingPolicy(rollingPolicy);
        } else {
            // Time based rolling (default)
            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setFileNamePattern(buildFileNamePattern(rotationConfig));
            rollingPolicy.setMaxHistory(rotationConfig.getMaxHistory());
            rollingPolicy.setParent(appender);
            rollingPolicy.start();
            appender.setRollingPolicy(rollingPolicy);
        }
    }

    private static String buildFileNamePattern(AccessLogRotationConfig rotationConfig) {
        String path = rotationConfig.getFileNamePattern();
        // If pattern doesn't contain directory prefix, add it from output path
        if (!path.contains("/") && !path.contains("\\")) {
            String outputPath = rotationConfig.getFileNamePattern();
            return outputPath;
        }
        return path;
    }

    private static Appender<ILoggingEvent> wrapWithAsync(
            RollingFileAppender<ILoggingEvent> appender,
            AccessLogAsyncConfig asyncConfig,
            LoggerContext loggerContext) {

        ch.qos.logback.classic.AsyncAppender asyncAppender = new ch.qos.logback.classic.AsyncAppender();
        asyncAppender.setContext(loggerContext);
        asyncAppender.setName("AsyncAccessLogAppender");
        asyncAppender.setQueueSize(asyncConfig.getQueueSize());
        asyncAppender.setDiscardingThreshold(asyncConfig.getDiscardingThreshold());
        asyncAppender.setNeverBlock(asyncConfig.isNeverBlock());
        asyncAppender.addAppender(appender);
        asyncAppender.start();

        return asyncAppender;
    }

    public static void stopAppender(Appender<?> appender) {
        if (appender != null) {
            appender.stop();
        }
    }
}
