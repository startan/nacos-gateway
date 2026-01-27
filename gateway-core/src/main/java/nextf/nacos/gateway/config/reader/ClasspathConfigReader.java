package nextf.nacos.gateway.config.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 类路径配置读取器
 * 从classpath读取配置，不支持热更新（classpath资源通常打包在jar中）
 */
public class ClasspathConfigReader implements ConfigFileReader {

    private static final Logger log = LoggerFactory.getLogger(ClasspathConfigReader.class);

    private final String resourcePath;
    private final ClassLoader classLoader;

    public ClasspathConfigReader(String resourcePath) {
        this.resourcePath = removeProtocolPrefix(resourcePath);
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public String readConfig() throws IOException {
        log.debug("Reading config from classpath: {}", resourcePath);

        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Configuration resource not found in classpath: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public void watchConfig(Runnable callback) {
        log.info("Classpath resources do not support hot reloading. Watching disabled for: {}", resourcePath);
        // Classpath资源不支持热更新，通常打包在jar中不可变
    }

    @Override
    public void stopWatching() {
        // 无需停止，因为没有启动监听
    }

    @Override
    public String getSourceDescription() {
        return "classpath://" + resourcePath;
    }

    private String removeProtocolPrefix(String path) {
        if (path.startsWith("classpath://")) {
            return path.substring(12);
        }
        return path;
    }
}
