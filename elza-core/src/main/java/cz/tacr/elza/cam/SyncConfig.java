package cz.tacr.elza.cam;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Deprecated(since = "2.0", forRemoval = true)
@Configuration
@ConfigurationProperties(prefix = "elza.accesspoints")
public class SyncConfig {

	public static class SynchronizationInfo {
        String code;
        Integer syncDelay;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getSyncDelay() { return syncDelay; }
        public void setSyncDelay(Integer syncDelay) { this.syncDelay = syncDelay; }
    }

    List<SynchronizationInfo> sync;

    public List<SynchronizationInfo> getSync() {
        return sync;
    }

    public void setSync(List<SynchronizationInfo> sync) {
        this.sync = sync;
    }

    public SynchronizationInfo getConfig(String code) {
        for(SynchronizationInfo s: sync) {
            if (Objects.equals(s.code, code)) {
                return s;
            }
        }
        return null;
    }

    public List<SynchronizationInfo> getConfig() {
        return sync;
    }
}
