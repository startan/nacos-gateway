package pans.gateway.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.vertx.core.json.JsonObject;

/**
 * JSON log formatter
 */
public class JsonLogFormatter implements LogFormatter {

    private final ObjectMapper mapper;

    public JsonLogFormatter() {
        this.mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String format(AccessLog log) {
        JsonObject json = new JsonObject()
                .put("timestamp", log.getTimestamp().toString())
                .put("level", log.getLevel())
                .put("method", log.getMethod())
                .put("path", log.getPath())
                .put("status", log.getStatus())
                .put("duration", log.getDuration())
                .put("clientIp", log.getClientIp())
                .put("backend", log.getBackend())
                .put("endpoint", log.getEndpoint());

        return json.encode();
    }
}
