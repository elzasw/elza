package cz.tacr.elza.service.cam;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// @ConfigurationProperties(prefix = "elza.ap-external-systems")
@ConfigurationProperties(prefix = "elza.accesspoints")
@Configuration
public class SyncConfig {
    public static class SynchronizationInfo {

        String code;
        String syncAt;
        /**
         * Number of secs between each synchronization
         */
        Integer syncDelay;
        String resetAt;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSyncAt() {
            return syncAt;
        }

        public void setSyncAt(String syncAt) {
            this.syncAt = syncAt;
        }

        public Integer getSyncDelay() {
            return syncDelay;
        }

        public void setSyncDelay(Integer syncDelay) {
            this.syncDelay = syncDelay;
        }

        public String getResetAt() {
            return resetAt;
        }

        public void setResetAt(String resetAt) {
            this.resetAt = resetAt;
        }

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
