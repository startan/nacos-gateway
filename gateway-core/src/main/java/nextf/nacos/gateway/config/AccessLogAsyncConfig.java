package nextf.nacos.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Access log async configuration
 */
public class AccessLogAsyncConfig {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("queueSize")
    private int queueSize = 512;

    @JsonProperty("discardingThreshold")
    private int discardingThreshold = 20;

    @JsonProperty("neverBlock")
    private boolean neverBlock = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getDiscardingThreshold() {
        return discardingThreshold;
    }

    public void setDiscardingThreshold(int discardingThreshold) {
        this.discardingThreshold = discardingThreshold;
    }

    public boolean isNeverBlock() {
        return neverBlock;
    }

    public void setNeverBlock(boolean neverBlock) {
        this.neverBlock = neverBlock;
    }

    @Override
    public String toString() {
        return "AccessLogAsyncConfig{" +
                "enabled=" + enabled +
                ", queueSize=" + queueSize +
                ", discardingThreshold=" + discardingThreshold +
                ", neverBlock=" + neverBlock +
                '}';
    }
}
