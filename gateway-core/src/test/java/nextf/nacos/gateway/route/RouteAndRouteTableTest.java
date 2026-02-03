package nextf.nacos.gateway.route;

import nextf.nacos.gateway.config.RateLimitConfig;
import nextf.nacos.gateway.config.RouteConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Route and RouteTable
 * Tests route creation, factory methods, equals/hashCode, and route table operations
 */
@DisplayName("Route and RouteTable Tests")
class RouteAndRouteTableTest {

    private RouteConfig routeConfig;
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setMaxQps(100);
        rateLimitConfig.setMaxConnections(50);

        routeConfig = new RouteConfig();
        routeConfig.setHost("*.example.com");
        routeConfig.setBackend("backend-service");
        routeConfig.setRateLimit(rateLimitConfig);
    }

    // ==================== Route Tests ====================

    @Test
    @DisplayName("Should create Route from RouteConfig")
    void testRouteCreation() {
        // Act
        Route route = new Route(routeConfig);

        // Assert
        assertThat(route).isNotNull();
        assertThat(route.getHostPattern()).isEqualTo("*.example.com");
        assertThat(route.getBackendName()).isEqualTo("backend-service");
        assertThat(route.getRateLimitConfig()).isSameAs(rateLimitConfig);
    }

    @Test
    @DisplayName("Should use host pattern as route ID")
    void testRouteId() {
        // Arrange
        routeConfig.setHost("api.example.com");

        // Act
        Route route = new Route(routeConfig);

        // Assert
        assertThat(route.getId()).isEqualTo("api.example.com");
        assertThat(route.getId()).isEqualTo(route.getHostPattern());
    }

    @Test
    @DisplayName("Should handle RouteConfig with null rate limit")
    void testRouteWithNullRateLimit() {
        // Arrange
        routeConfig.setRateLimit(null);

        // Act
        Route route = new Route(routeConfig);

        // Assert
        assertThat(route.getRateLimitConfig()).isNull();
        assertThat(route.getHostPattern()).isEqualTo("*.example.com");
        assertThat(route.getBackendName()).isEqualTo("backend-service");
    }

    @Test
    @DisplayName("Should create route map from list of configs")
    void testRouteFromList() {
        // Arrange
        RouteConfig config1 = new RouteConfig();
        config1.setHost("api.example.com");
        config1.setBackend("backend1");

        RouteConfig config2 = new RouteConfig();
        config2.setHost("web.example.com");
        config2.setBackend("backend2");

        List<RouteConfig> configs = List.of(config1, config2);

        // Act
        Map<String, Route> routesMap = Route.from(configs);

        // Assert
        assertThat(routesMap).hasSize(2);
        assertThat(routesMap).containsKeys("api.example.com", "web.example.com");

        Route route1 = routesMap.get("api.example.com");
        assertThat(route1.getBackendName()).isEqualTo("backend1");

        Route route2 = routesMap.get("web.example.com");
        assertThat(route2.getBackendName()).isEqualTo("backend2");
    }

    @Test
    @DisplayName("Should handle null list when creating route map")
    void testRouteFromNullList() {
        // Act
        Map<String, Route> routesMap = Route.from(null);

        // Assert
        assertThat(routesMap).isNotNull();
        assertThat(routesMap).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty list when creating route map")
    void testRouteFromEmptyList() {
        // Act
        Map<String, Route> routesMap = Route.from(new ArrayList<>());

        // Assert
        assertThat(routesMap).isNotNull();
        assertThat(routesMap).isEmpty();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void testRouteEquals() {
        // Arrange
        Route route1 = new Route(routeConfig);
        Route route2 = new Route(routeConfig);

        RouteConfig differentConfig = new RouteConfig();
        differentConfig.setHost("*.other.com");
        differentConfig.setBackend("other-backend");
        Route route3 = new Route(differentConfig);

        // Assert
        assertThat(route1).isEqualTo(route2);
        assertThat(route1).isNotEqualTo(route3);
        assertThat(route1).isNotEqualTo(null);
        assertThat(route1).isNotEqualTo("string");
    }

    @Test
    @DisplayName("Should have same hashCode for equal routes")
    void testRouteHashCode() {
        // Arrange
        Route route1 = new Route(routeConfig);
        Route route2 = new Route(routeConfig);

        // Assert
        assertThat(route1.hashCode()).isEqualTo(route2.hashCode());
    }

    @Test
    @DisplayName("Should have different hashCode for different routes")
    void testRouteHashCodeDifferent() {
        // Arrange
        Route route1 = new Route(routeConfig);

        RouteConfig differentConfig = new RouteConfig();
        differentConfig.setHost("*.other.com");
        differentConfig.setBackend("other-backend");
        Route route2 = new Route(differentConfig);

        // Assert
        assertThat(route1.hashCode()).isNotEqualTo(route2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void testRouteToString() {
        // Arrange
        Route route = new Route(routeConfig);

        // Act
        String str = route.toString();

        // Assert
        assertThat(str).contains("*.example.com");
        assertThat(str).contains("backend-service");
        assertThat(str).contains("rateLimitConfig");
    }

    // ==================== RouteTable Tests ====================

    @Test
    @DisplayName("Should create empty RouteTable")
    void testEmptyRouteTable() {
        // Act
        RouteTable routeTable = new RouteTable();

        // Assert
        assertThat(routeTable).isNotNull();
        assertThat(routeTable.size()).isZero();
        assertThat(routeTable.getAllRoutes()).isEmpty();
    }

    @Test
    @DisplayName("Should create RouteTable from list of routes")
    void testRouteTableFromList() {
        // Arrange
        Route route1 = new Route(routeConfig);

        RouteConfig config2 = new RouteConfig();
        config2.setHost("web.example.com");
        config2.setBackend("backend2");
        Route route2 = new Route(config2);

        List<Route> routes = List.of(route1, route2);

        // Act
        RouteTable routeTable = new RouteTable(routes);

        // Assert
        assertThat(routeTable.size()).isEqualTo(2);
        assertThat(routeTable.getRoute("*.example.com")).isSameAs(route1);
        assertThat(routeTable.getRoute("web.example.com")).isSameAs(route2);
    }

    @Test
    @DisplayName("Should handle null list when creating RouteTable")
    void testRouteTableFromNullList() {
        // Act
        RouteTable routeTable = new RouteTable(null);

        // Assert
        assertThat(routeTable.size()).isZero();
    }

    @Test
    @DisplayName("Should add route to table")
    void testAddRoute() {
        // Arrange
        RouteTable routeTable = new RouteTable();
        Route route = new Route(routeConfig);

        // Act
        routeTable.addRoute(route);

        // Assert
        assertThat(routeTable.size()).isEqualTo(1);
        assertThat(routeTable.getRoute("*.example.com")).isSameAs(route);
    }

    @Test
    @DisplayName("Should remove route from table")
    void testRemoveRoute() {
        // Arrange
        RouteTable routeTable = new RouteTable();
        Route route = new Route(routeConfig);
        routeTable.addRoute(route);

        // Act
        routeTable.removeRoute("*.example.com");

        // Assert
        assertThat(routeTable.size()).isZero();
        assertThat(routeTable.getRoute("*.example.com")).isNull();
    }

    @Test
    @DisplayName("Should handle removing non-existent route")
    void testRemoveNonExistentRoute() {
        // Arrange
        RouteTable routeTable = new RouteTable();

        // Act
        routeTable.removeRoute("non-existent");

        // Assert - should not throw
        assertThat(routeTable.size()).isZero();
    }

    @Test
    @DisplayName("Should get route by ID")
    void testGetRoute() {
        // Arrange
        RouteTable routeTable = new RouteTable();
        Route route = new Route(routeConfig);
        routeTable.addRoute(route);

        // Act
        Route retrieved = routeTable.getRoute("*.example.com");

        // Assert
        assertThat(retrieved).isSameAs(route);
    }

    @Test
    @DisplayName("Should return null for non-existent route")
    void testGetNonExistentRoute() {
        // Arrange
        RouteTable routeTable = new RouteTable();

        // Act
        Route retrieved = routeTable.getRoute("non-existent");

        // Assert
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("Should get all routes")
    void testGetAllRoutes() {
        // Arrange
        Route route1 = new Route(routeConfig);

        RouteConfig config2 = new RouteConfig();
        config2.setHost("web.example.com");
        config2.setBackend("backend2");
        Route route2 = new Route(config2);

        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route1);
        routeTable.addRoute(route2);

        // Act
        List<Route> allRoutes = routeTable.getAllRoutes();

        // Assert
        assertThat(allRoutes).hasSize(2);
        assertThat(allRoutes).contains(route1, route2);
    }

    @Test
    @DisplayName("Should return copy of routes list")
    void testGetAllRoutesReturnsCopy() {
        // Arrange
        Route route = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route);

        // Act
        List<Route> allRoutes = routeTable.getAllRoutes();
        allRoutes.clear();

        // Assert - original table should be unchanged
        assertThat(routeTable.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should clear all routes")
    void testClear() {
        // Arrange
        Route route = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route);

        // Act
        routeTable.clear();

        // Assert
        assertThat(routeTable.size()).isZero();
        assertThat(routeTable.getAllRoutes()).isEmpty();
    }

    @Test
    @DisplayName("Should update routes")
    void testUpdateRoutes() {
        // Arrange
        Route route1 = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route1);

        RouteConfig config2 = new RouteConfig();
        config2.setHost("new.example.com");
        config2.setBackend("new-backend");
        Route route2 = new Route(config2);

        List<Route> newRoutes = List.of(route2);

        // Act
        routeTable.updateRoutes(newRoutes);

        // Assert
        assertThat(routeTable.size()).isEqualTo(1);
        assertThat(routeTable.getRoute("*.example.com")).isNull();
        assertThat(routeTable.getRoute("new.example.com")).isSameAs(route2);
    }

    @Test
    @DisplayName("Should clear table when updating with null")
    void testUpdateRoutesWithNull() {
        // Arrange
        Route route = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route);

        // Act
        routeTable.updateRoutes(null);

        // Assert
        assertThat(routeTable.size()).isZero();
    }

    @Test
    @DisplayName("Should clear table when updating with empty list")
    void testUpdateRoutesWithEmptyList() {
        // Arrange
        Route route = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route);

        // Act
        routeTable.updateRoutes(new ArrayList<>());

        // Assert
        assertThat(routeTable.size()).isZero();
    }

    @Test
    @DisplayName("Should update routes with multiple entries")
    void testUpdateRoutesMultiple() {
        // Arrange
        Route route1 = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route1);

        RouteConfig config2 = new RouteConfig();
        config2.setHost("api.example.com");
        config2.setBackend("backend2");

        RouteConfig config3 = new RouteConfig();
        config3.setHost("web.example.com");
        config3.setBackend("backend3");

        List<Route> newRoutes = List.of(
                new Route(config2),
                new Route(config3)
        );

        // Act
        routeTable.updateRoutes(newRoutes);

        // Assert
        assertThat(routeTable.size()).isEqualTo(2);
        assertThat(routeTable.getRoute("*.example.com")).isNull();
        assertThat(routeTable.getRoute("api.example.com")).isNotNull();
        assertThat(routeTable.getRoute("web.example.com")).isNotNull();
    }

    @Test
    @DisplayName("Should override existing route when adding with same ID")
    void testOverrideRoute() {
        // Arrange
        Route route1 = new Route(routeConfig);
        RouteTable routeTable = new RouteTable();
        routeTable.addRoute(route1);

        RouteConfig config2 = new RouteConfig();
        config2.setHost("*.example.com"); // Same host pattern (ID)
        config2.setBackend("different-backend");
        Route route2 = new Route(config2);

        // Act
        routeTable.addRoute(route2);

        // Assert
        assertThat(routeTable.size()).isEqualTo(1);
        assertThat(routeTable.getRoute("*.example.com").getBackendName()).isEqualTo("different-backend");
    }
}
