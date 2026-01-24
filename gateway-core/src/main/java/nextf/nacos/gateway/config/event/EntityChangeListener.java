package nextf.nacos.gateway.config.event;

/**
 * Entity change listener interface
 * Components implement this interface to receive notifications about entity changes
 */
public interface EntityChangeListener {
    /**
     * Called when entities (routes or backends) are updated
     * @param event The change event
     */
    void onEntityChanged(EntityChangeEvent event);
}
