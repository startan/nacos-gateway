package nextf.nacos.gateway.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimitConfig
 */
public class RateLimitConfigTest {

    @Test
    public void testIsDefaultUnlimited_AllDefaults() {
        RateLimitConfig config = new RateLimitConfig();
        assertTrue(config.isDefaultUnlimited(),
            "New RateLimitConfig instance should have all default values (-1)");
    }

    @Test
    public void testIsDefaultUnlimited_OneNonDefault() {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxQps(100);
        assertFalse(config.isDefaultUnlimited(),
            "Config with non-zero maxQps should not be default unlimited");
    }

    @Test
    public void testIsDefaultUnlimited_AllNonDefaults() {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxQps(100);
        config.setMaxConnections(1000);
        config.setMaxQpsPerClient(10);
        config.setMaxConnectionsPerClient(5);
        assertFalse(config.isDefaultUnlimited(),
            "Config with all non-default values should not be default unlimited");
    }

    @Test
    public void testIsDefaultUnlimited_ZeroIsNotDefault() {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxQps(0);
        assertFalse(config.isDefaultUnlimited(),
            "Config with maxQps=0 (reject all) should not be default unlimited");
    }

    @Test
    public void testBackendConfig_HasDefaultRateLimitInstance() {
        BackendConfig config = new BackendConfig();
        assertNotNull(config.getRateLimit(),
            "BackendConfig should have non-null rateLimit instance");
        assertTrue(config.getRateLimit().isDefaultUnlimited(),
            "BackendConfig default rateLimit should be unlimited");
    }

    @Test
    public void testRouteConfig_HasDefaultRateLimitInstance() {
        RouteConfig config = new RouteConfig();
        assertNotNull(config.getRateLimit(),
            "RouteConfig should have non-null rateLimit instance");
        assertTrue(config.getRateLimit().isDefaultUnlimited(),
            "RouteConfig default rateLimit should be unlimited");
    }

    @Test
    public void testServerConfig_HasDefaultRateLimitInstance() {
        ServerConfig config = new ServerConfig();
        assertNotNull(config.getRateLimit(),
            "ServerConfig should have non-null rateLimit instance");
        assertTrue(config.getRateLimit().isDefaultUnlimited(),
            "ServerConfig default rateLimit should be unlimited");
    }

    @Test
    public void testRateLimitConfig_HelperMethods() {
        RateLimitConfig config = new RateLimitConfig();

        // Test default unlimited state
        assertFalse(config.isQpsLimited(), "Default config should not have QPS limit");
        assertFalse(config.isConnectionsLimited(), "Default config should not have connections limit");
        assertFalse(config.isQpsPerClientLimited(), "Default config should not have per-client QPS limit");
        assertFalse(config.isConnectionsPerClientLimited(), "Default config should not have per-client connections limit");
        assertFalse(config.isQpsRejected(), "Default config should not reject QPS");
        assertFalse(config.isConnectionsRejected(), "Default config should not reject connections");

        // Test with limits
        config.setMaxQps(100);
        assertTrue(config.isQpsLimited(), "Config with maxQps=100 should have QPS limit");
        assertFalse(config.isQpsRejected(), "Config with maxQps=100 should not reject QPS");

        config.setMaxConnections(1000);
        assertTrue(config.isConnectionsLimited(), "Config with maxConnections=1000 should have connections limit");

        config.setMaxQpsPerClient(10);
        assertTrue(config.isQpsPerClientLimited(), "Config with maxQpsPerClient=10 should have per-client QPS limit");

        config.setMaxConnectionsPerClient(5);
        assertTrue(config.isConnectionsPerClientLimited(), "Config with maxConnectionsPerClient=5 should have per-client connections limit");

        // Test reject state
        config.setMaxQps(0);
        assertTrue(config.isQpsRejected(), "Config with maxQps=0 should reject QPS");

        config.setMaxConnections(0);
        assertTrue(config.isConnectionsRejected(), "Config with maxConnections=0 should reject connections");
    }
}
