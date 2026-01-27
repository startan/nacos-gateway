package nextf.nacos.gateway.config.reader;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置文件读取器工厂
 * 根据配置路径协议返回对应的读取器实例
 */
public class ConfigFileReaderFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigFileReaderFactory.class);

    /**
     * 根据配置路径获取对应的读取器
     * @param configPath 配置路径（支持 file://, classpath://, nacos:// 协议）
     * @param vertx Vertx实例（file协议需要）
     * @return ConfigFileReader实例
     */
    public static ConfigFileReader getReader(String configPath, Vertx vertx) throws Exception {
        if (configPath == null || configPath.isEmpty()) {
            throw new IllegalArgumentException("Config path cannot be null or empty");
        }

        log.info("Creating config reader for: {}", configPath);

        // 判断协议类型
        if (configPath.startsWith("file://")) {
            return new FileConfigReader(vertx, configPath);

        } else if (configPath.startsWith("classpath://")) {
            return new ClasspathConfigReader(configPath);

        } else if (configPath.startsWith("nacos://")) {
            return new NacosConfigReader(configPath);

        } else {
            // 默认使用 file:// 协议（向后兼容）
            log.debug("No protocol specified, defaulting to file:// protocol");
            return new FileConfigReader(vertx, "file://" + configPath);
        }
    }
}
