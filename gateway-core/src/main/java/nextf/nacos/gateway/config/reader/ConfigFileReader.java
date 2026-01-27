package nextf.nacos.gateway.config.reader;

import java.io.IOException;

/**
 * 配置文件读取器接口
 * 支持从不同来源读取配置并监听配置变化
 */
public interface ConfigFileReader {

    /**
     * 读取配置内容
     * @return 配置文件的完整内容字符串
     * @throws IOException 读取失败时抛出
     */
    String readConfig() throws IOException;

    /**
     * 监听配置变化
     * @param callback 配置变化时的回调函数
     */
    void watchConfig(Runnable callback);

    /**
     * 停止监听并释放资源
     */
    void stopWatching();

    /**
     * 获取配置来源描述
     * @return 配置来源的描述字符串（用于日志）
     */
    String getSourceDescription();
}
