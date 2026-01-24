package nextf.nacos.gateway.config.event;

import nextf.nacos.gateway.route.Route;

import java.util.Map;

/**
 * Event fired when routes are updated
 */
public class RoutesUpdatedEvent extends EntityChangeEvent {
    private final Map<String, Route> oldRoutes;
    private final Map<String, Route> newRoutes;

    public RoutesUpdatedEvent(long version,
                              Map<String, Route> oldRoutes,
                              Map<String, Route> newRoutes) {
        super(version);
        this.oldRoutes = oldRoutes;
        this.newRoutes = newRoutes;
    }

    /**
     * Get the old routes before the update
     * @return Map of old routes (may be null)
     */
    public Map<String, Route> getOldRoutes() {
        return oldRoutes;
    }

    /**
     * Get the new routes after the update
     * @return Map of new routes
     */
    public Map<String, Route> getNewRoutes() {
        return newRoutes;
    }
}
