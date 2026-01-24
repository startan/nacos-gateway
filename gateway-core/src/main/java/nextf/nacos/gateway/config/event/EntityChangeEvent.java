package nextf.nacos.gateway.config.event;

/**
 * Base class for entity change events
 */
public abstract class EntityChangeEvent {
    private final long version;
    private final long timestamp;

    protected EntityChangeEvent(long version) {
        this.version = version;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the configuration version
     * @return The version number
     */
    public long getVersion() {
        return version;
    }

    /**
     * Get the timestamp when this event was created
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
}
