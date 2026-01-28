package nextf.nacos.gateway.config.reader;

import java.io.IOException;

/**
 * Configuration file reader interface
 * Supports reading configuration from different sources and listening for configuration changes
 */
public interface ConfigFileReader {

    /**
     * Read configuration content
     * @return Complete content string of the configuration file
     * @throws IOException Thrown when read fails
     */
    String readConfig() throws IOException;

    /**
     * Listen for configuration changes
     * @param callback Callback function when configuration changes
     */
    void watchConfig(Runnable callback);

    /**
     * Stop listening and release resources
     */
    void stopWatching();

    /**
     * Get configuration source description
     * @return Description string of configuration source (for logging)
     */
    String getSourceDescription();
}
