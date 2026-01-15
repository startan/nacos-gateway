package pans.gateway.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pans.gateway.config.BackendConfig;
import pans.gateway.config.RouteConfig;
import pans.gateway.config.event.BackendsUpdatedEvent;
import pans.gateway.config.event.EntityChangeListener;
import pans.gateway.config.event.EntityChangeEvent;
import pans.gateway.config.event.RoutesUpdatedEvent;
import pans.gateway.loadbalance.EndpointSelector;
import pans.gateway.model.Backend;
import pans.gateway.model.Endpoint;
import pans.gateway.route.Route;
import pans.gateway.route.RouteMatcher;
import pans.gateway.route.RouteMatcherImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gateway Registry - Central registry for runtime entities (Routes, Backends, Endpoints)
 * Manages entity lifecycle, provides query interfaces, and publishes change events
 */
public class GatewayRegistry {

    private static final Logger log = LoggerFactory.getLogger(GatewayRegistry.class);

    // Entity storage (volatile for atomic visibility)
    private volatile Map<String, Route> routes;
    private volatile Map<String, Backend> backends;
    private volatile RouteMatcher routeMatcher;

    // For rollback
    private Map<String, Route> previousRoutes;
    private Map<String, Backend> previousBackends;

    // Version tracking
    private final AtomicLong version = new AtomicLong(0);

    // Event listeners
    private final List<EntityChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Default constructor
     */
    public GatewayRegistry() {
        this.routes = new ConcurrentHashMap<>();
        this.backends = new ConcurrentHashMap<>();
        this.routeMatcher = new RouteMatcherImpl(new ArrayList<>());
    }

    // ========== Update Methods ==========

    /**
     * Update routes from configuration
     * @param routeConfigs List of route configurations
     */
    public void updateRoutes(List<RouteConfig> routeConfigs) {
        long newVersion = version.incrementAndGet();
        log.info("Updating routes to version {}", newVersion);

        try {
            // Build new routes using static factory
            Map<String, Route> newRoutes = Route.from(routeConfigs);

            // Save snapshot for rollback
            previousRoutes = this.routes;

            // Create new route matcher
            RouteMatcher newMatcher = new RouteMatcherImpl(new ArrayList<>(newRoutes.values()));

            // Atomic swap
            this.routes = newRoutes;
            this.routeMatcher = newMatcher;

            // Publish event if routes actually changed
            if (!routesEqual(previousRoutes, newRoutes)) {
                RoutesUpdatedEvent event = new RoutesUpdatedEvent(newVersion, previousRoutes, newRoutes);
                notifyListeners(event);
            }

            log.info("Routes updated successfully to version {}, {} routes", newVersion, newRoutes.size());

        } catch (Exception e) {
            // Rollback on error
            log.error("Failed to update routes, rolling back", e);
            rollback();
            version.decrementAndGet();
            throw new RuntimeException("Failed to update routes", e);
        }
    }

    /**
     * Update backends from configuration
     * @param backendConfigs List of backend configurations
     */
    public void updateBackends(List<BackendConfig> backendConfigs) {
        long newVersion = version.incrementAndGet();
        log.info("Updating backends to version {}", newVersion);

        try {
            // Build new backends using static factory
            Map<String, Backend> newBackends = Backend.fromList(backendConfigs);

            // Save snapshot for rollback
            previousBackends = this.backends;

            // Atomic swap
            this.backends = newBackends;

            // Publish event if backends actually changed
            if (!backendsEqual(previousBackends, newBackends)) {
                BackendsUpdatedEvent event = new BackendsUpdatedEvent(newVersion, previousBackends, newBackends);
                notifyListeners(event);
            }

            log.info("Backends updated successfully to version {}, {} backends", newVersion, newBackends.size());

        } catch (Exception e) {
            // Rollback on error
            log.error("Failed to update backends, rolling back", e);
            rollback();
            version.decrementAndGet();
            throw new RuntimeException("Failed to update backends", e);
        }
    }

    // ========== Query Methods ==========

    /**
     * Get route by ID
     * @param id Route ID (host pattern)
     * @return Route or null if not found
     */
    public Route getRoute(String id) {
        return routes.get(id);
    }

    /**
     * Get backend by name
     * @param name Backend name
     * @return Backend or null if not found
     */
    public Backend getBackend(String name) {
        return backends.get(name);
    }

    /**
     * Get all routes
     * @return Map of route ID to Route
     */
    public Map<String, Route> getRoutes() {
        return routes;
    }

    /**
     * Get all backends
     * @return Map of backend name to Backend
     */
    public Map<String, Backend> getBackends() {
        return backends;
    }

    /**
     * Get route matcher
     * @return RouteMatcher instance
     */
    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }

    /**
     * Get endpoints for a specific backend
     * @param backendName Backend name
     * @return List of endpoints (empty list if backend not found)
     */
    public List<Endpoint> getEndpoints(String backendName) {
        Backend backend = backends.get(backendName);
        return backend != null ? backend.getEndpoints() : new ArrayList<>();
    }

    /**
     * Get current version
     * @return Current version number
     */
    public long getVersion() {
        return version.get();
    }

    // ========== Listener Management ==========

    /**
     * Register an entity change listener
     * @param listener The listener to register
     */
    public void registerListener(EntityChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Registered entity change listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister an entity change listener
     * @param listener The listener to unregister
     */
    public void unregisterListener(EntityChangeListener listener) {
        if (listeners.remove(listener)) {
            log.debug("Unregistered entity change listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all listeners of an entity change event
     * @param event The event to publish
     */
    private void notifyListeners(EntityChangeEvent event) {
        log.debug("Notifying {} listeners of event: {}", listeners.size(), event.getClass().getSimpleName());
        for (EntityChangeListener listener : listeners) {
            try {
                listener.onEntityChanged(event);
            } catch (Exception e) {
                log.error("Error notifying listener {}: {}", listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    // ========== Helper Methods ==========

    /**
     * Rollback to previous state
     */
    private void rollback() {
        log.warn("Rolling back to previous state...");
        if (previousRoutes != null) {
            this.routes = previousRoutes;
            this.routeMatcher = new RouteMatcherImpl(new ArrayList<>(previousRoutes.values()));
        }
        if (previousBackends != null) {
            this.backends = previousBackends;
        }
    }

    /**
     * Compare two route maps for equality
     */
    private boolean routesEqual(Map<String, Route> r1, Map<String, Route> r2) {
        if (r1 == null && r2 == null) return true;
        if (r1 == null || r2 == null) return false;
        return r1.size() == r2.size() && r1.keySet().equals(r2.keySet());
    }

    /**
     * Compare two backend maps for equality
     */
    private boolean backendsEqual(Map<String, Backend> b1, Map<String, Backend> b2) {
        if (b1 == null && b2 == null) return true;
        if (b1 == null || b2 == null) return false;
        return b1.size() == b2.size() && b1.keySet().equals(b2.keySet());
    }
}
