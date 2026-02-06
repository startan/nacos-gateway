package nextf.nacos.gateway.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Access log event for Logback
 * Implements ILoggingEvent to integrate with Logback's appenders
 */
public class AccessLogEvent extends ContextAwareBase implements ILoggingEvent {

    private final AccessLogContext context;
    private final LoggerContextVO loggerContextVO;
    private final long sequenceNumber;

    public AccessLogEvent(AccessLogContext context) {
        this.context = context;
        // Use the correct LoggerContextVO constructor signature
        this.loggerContextVO = new LoggerContextVO(
                "default",
                new HashMap<>(),
                System.currentTimeMillis()
        );
        this.sequenceNumber = System.nanoTime();
    }

    public AccessLogContext getAccessLogContext() {
        return context;
    }

    @Override
    public String getThreadName() {
        return Thread.currentThread().getName();
    }

    @Override
    public Level getLevel() {
        return Level.INFO;
    }

    @Override
    public String getMessage() {
        return "AccessLog";
    }

    @Override
    public Object[] getArgumentArray() {
        return null;
    }

    @Override
    public String getFormattedMessage() {
        return "AccessLog";
    }

    @Override
    public String getLoggerName() {
        return "AccessLog";
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return loggerContextVO;
    }

    @Override
    public ThrowableProxy getThrowableProxy() {
        return null;
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return new StackTraceElement[0];
    }

    @Override
    public boolean hasCallerData() {
        return false;
    }

    @Override
    public Marker getMarker() {
        return null;
    }

    @Override
    public List<Marker> getMarkerList() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getMdc() {
        return getMDCPropertyMap();
    }

    @Override
    public long getTimeStamp() {
        return context.getTimestamp().toEpochMilli();
    }

    @Override
    public void prepareForDeferredProcessing() {
        // No preparation needed
    }

    // Newer Logback versions require this method
    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return List.of(
                new KeyValuePair("clientIp", context.getClientIp()),
                new KeyValuePair("method", context.getMethod()),
                new KeyValuePair("uri", context.getUri()),
                new KeyValuePair("status", String.valueOf(context.getStatus())),
                new KeyValuePair("durationMs", String.valueOf(context.getDurationMs())),
                new KeyValuePair("backend", context.getBackend()),
                new KeyValuePair("endpoint", context.getEndpoint())
        );
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public int getNanoseconds() {
        return (int) (sequenceNumber % 1000000);
    }
}
