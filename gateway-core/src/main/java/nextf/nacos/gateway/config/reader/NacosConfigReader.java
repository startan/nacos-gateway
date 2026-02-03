package nextf.nacos.gateway.config.reader;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nacos configuration center reader
 * Reads configuration from Nacos configuration center, supports real-time push updates
 */
public class NacosConfigReader implements ConfigFileReader {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigReader.class);

    private final NacosUrlConfig urlConfig;
    private final ConfigService configService;
    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    private volatile String currentConfig;
    private Listener listener;

    public NacosConfigReader(String nacosUrl) throws IOException {
        this.urlConfig = NacosUrlParser.parse(nacosUrl);

        try {
            // Create ConfigService
            configService = ConfigFactory.createConfigService(urlConfig.getProperties());

            // Initial configuration load
            this.currentConfig = configService.getConfig(
                urlConfig.getDataId(),
                urlConfig.getGroup(),
                5000 // 5 seconds timeout
            );

            if (currentConfig == null) {
                throw new IOException("Configuration not found in Nacos: " + urlConfig);
            }

            log.info("Successfully loaded config from Nacos: dataId={}, group={}",
                urlConfig.getDataId(), urlConfig.getGroup());

        } catch (NacosException e) {
            throw new IOException("Failed to initialize Nacos config reader: " + e.getMessage(), e);
        }
    }

    @Override
    public String readConfig() throws IOException {
        return currentConfig;
    }

    @Override
    public void watchConfig(Runnable callback) {
        if (isWatching.getAndSet(true)) {
            log.warn("Already watching Nacos config, ignoring duplicate watch request");
            return;
        }

        try {
            listener = new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("Received config update from Nacos: dataId={}, group={}",
                        urlConfig.getDataId(), urlConfig.getGroup());

                    currentConfig = configInfo;
                    callback.run();
                }

                @Override
                public Executor getExecutor() {
                    // Use Nacos default thread pool to execute callback
                    return null;
                }
            };

            configService.addListener(urlConfig.getDataId(), urlConfig.getGroup(), listener);

            log.info("Nacos config listener registered successfully: dataId={}, group={}",
                urlConfig.getDataId(), urlConfig.getGroup());

        } catch (NacosException e) {
            log.error("Failed to add Nacos config listener: {}", e.getMessage(), e);
            isWatching.set(false);
        }
    }

    @Override
    public void stopWatching() {
        if (listener != null && configService != null) {
            try {
                configService.removeListener(urlConfig.getDataId(), urlConfig.getGroup(), listener);
                log.info("Nacos config listener removed");
            } catch (Exception e) {
                log.error("Error removing Nacos config listener: {}", e.getMessage(), e);
            }
        }

        isWatching.set(false);
    }

    @Override
    public String getSourceDescription() {
        // Build URL in new format: nacos://dataId:group?params
        StringBuilder sb = new StringBuilder("nacos://");
        sb.append(urlConfig.getDataId());

        // Add group if not default
        if (!"DEFAULT_GROUP".equals(urlConfig.getGroup())) {
            sb.append(":").append(urlConfig.getGroup());
        }

        // Add query parameters if any
        Properties props = urlConfig.getProperties();
        if (!props.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (String key : props.stringPropertyNames()) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(key).append("=").append(props.getProperty(key));
                first = false;
            }
        }

        return sb.toString();
    }
}
