package pans.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Route matcher implementation
 */
public class RouteMatcherImpl implements RouteMatcher {

    private static final Logger log = LoggerFactory.getLogger(RouteMatcherImpl.class);

    private final List<RouteMatchEntry> routes = new CopyOnWriteArrayList<>();

    public RouteMatcherImpl() {
    }

    public RouteMatcherImpl(List<Route> routeList) {
        if (routeList != null) {
            for (Route route : routeList) {
                addRoute(route);
            }
        }
    }

    public void addRoute(Route route) {
        HostMatcher hostMatcher = new HostMatcher(route.getHostPattern());
        routes.add(new RouteMatchEntry(route, hostMatcher));
        log.debug("Added route matcher for: {}", route);
    }

    public void clear() {
        routes.clear();
        log.debug("Cleared all route matchers");
    }

    public void updateRoutes(List<Route> newRoutes) {
        clear();
        if (newRoutes != null) {
            for (Route route : newRoutes) {
                addRoute(route);
            }
        }
        log.info("Updated {} route matchers", routes.size());
    }

    @Override
    public Optional<Route> match(String host) {
        if (host == null) {
            return Optional.empty();
        }

        // Find first matching route
        for (RouteMatchEntry entry : routes) {
            if (entry.hostMatcher.matches(host)) {
                log.debug("Route matched: host={}, route={}", host, entry.route);
                return Optional.of(entry.route);
            }
        }

        log.debug("No route matched: host={}", host);
        return Optional.empty();
    }

    private static class RouteMatchEntry {
        final Route route;
        final HostMatcher hostMatcher;

        RouteMatchEntry(Route route, HostMatcher hostMatcher) {
            this.route = route;
            this.hostMatcher = hostMatcher;
        }
    }
}
