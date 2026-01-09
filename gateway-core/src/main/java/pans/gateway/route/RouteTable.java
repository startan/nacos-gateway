package pans.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Route table for managing all routes
 */
public class RouteTable {

    private static final Logger log = LoggerFactory.getLogger(RouteTable.class);

    private final Map<String, Route> routes = new ConcurrentHashMap<>();

    public RouteTable() {
    }

    public RouteTable(List<Route> routeList) {
        if (routeList != null) {
            for (Route route : routeList) {
                addRoute(route);
            }
        }
    }

    public void addRoute(Route route) {
        routes.put(route.getId(), route);
        log.debug("Added route: {}", route);
    }

    public void removeRoute(String routeId) {
        Route removed = routes.remove(routeId);
        if (removed != null) {
            log.debug("Removed route: {}", removed);
        }
    }

    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }

    public void clear() {
        routes.clear();
        log.debug("Cleared all routes");
    }

    public void updateRoutes(List<Route> newRoutes) {
        clear();
        if (newRoutes != null) {
            for (Route route : newRoutes) {
                addRoute(route);
            }
        }
        log.info("Updated {} routes", routes.size());
    }

    public int size() {
        return routes.size();
    }
}
