package nextf.nacos.gateway.config.reader;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nacos配置中心读取器
 * 从Nacos配置中心读取配置，支持实时推送更新
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
            // 创建ConfigService
            configService = ConfigFactory.createConfigService(urlConfig.getProperties());

            // 初始加载配置
            this.currentConfig = configService.getConfig(
                urlConfig.getDataId(),
                urlConfig.getGroup(),
                5000 // 5秒超时
            );

            if (currentConfig == null) {
                throw new IOException("Configuration not found in Nacos: " + urlConfig);
            }

            log.info("Successfully loaded config from Nacos: dataId={}, group={}, namespace={}",
                urlConfig.getDataId(), urlConfig.getGroup(), urlConfig.getNamespace());

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
                    // 使用Nacos默认的线程池执行回调
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
        return String.format("nacos://%s?group=%s&namespace=%s&serverAddr=%s",
            urlConfig.getDataId(), urlConfig.getGroup(),
            urlConfig.getNamespace(), urlConfig.getServerAddr());
    }
}
