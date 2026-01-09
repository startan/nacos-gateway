package pans.gateway.route;

import java.util.Optional;

/**
 * Route matcher interface
 */
public interface RouteMatcher {

    /**
     * Match a route by host and path
     */
    Optional<Route> match(String host, String path);
}
