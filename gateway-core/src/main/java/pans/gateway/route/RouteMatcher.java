package pans.gateway.route;

import java.util.Optional;

/**
 * Route matcher interface
 */
public interface RouteMatcher {

    /**
     * Match a route by host
     */
    Optional<Route> match(String host);
}
