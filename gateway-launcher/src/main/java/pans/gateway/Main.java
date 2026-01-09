package pans.gateway;

import pans.gateway.server.ServerBootstrap;

/**
 * Main entry point for Nacos Gateway
 */
public class Main {

    public static void main(String[] args) {
        String configPath = getConfigPath(args);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap(configPath);
            bootstrap.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down Nacos Gateway...");
                bootstrap.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start Nacos Gateway: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getConfigPath(String[] args) {
        // Check if -c parameter is provided
        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            }
        }

        // Default config filenames
        String[] defaultConfigs = {"nacos-gateway.yaml", "nacos-gateway.yml"};

        // Check if config exists in classpath
        for (String configName : defaultConfigs) {
            String classpathConfig = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(configName) != null ? configName : null;

            if (classpathConfig != null) {
                return classpathConfig;
            }
        }

        // Check if config exists in current directory
        for (String configName : defaultConfigs) {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(configName))) {
                return configName;
            }
        }

        // Check in src/main/resources (for development)
        for (String configName : defaultConfigs) {
            String devPath = "src/main/resources/" + configName;
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(devPath))) {
                return devPath;
            }
        }

        // Return first default config as fallback
        return defaultConfigs[0];
    }
}
