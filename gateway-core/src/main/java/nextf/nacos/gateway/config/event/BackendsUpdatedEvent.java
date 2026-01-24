package nextf.nacos.gateway.config.event;

import nextf.nacos.gateway.model.Backend;

import java.util.Map;

/**
 * Event fired when backends are updated
 */
public class BackendsUpdatedEvent extends EntityChangeEvent {
    private final Map<String, Backend> oldBackends;
    private final Map<String, Backend> newBackends;

    public BackendsUpdatedEvent(long version,
                                Map<String, Backend> oldBackends,
                                Map<String, Backend> newBackends) {
        super(version);
        this.oldBackends = oldBackends;
        this.newBackends = newBackends;
    }

    /**
     * Get the old backends before the update
     * @return Map of old backends (may be null)
     */
    public Map<String, Backend> getOldBackends() {
        return oldBackends;
    }

    /**
     * Get the new backends after the update
     * @return Map of new backends
     */
    public Map<String, Backend> getNewBackends() {
        return newBackends;
    }
}
