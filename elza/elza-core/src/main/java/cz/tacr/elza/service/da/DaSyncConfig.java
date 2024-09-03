package cz.tacr.elza.service.da;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "elza.da")
@Configuration
public class DaSyncConfig {
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

    List<DaSyncConfig.SynchronizationInfo> sync;

    public List<DaSyncConfig.SynchronizationInfo> getSync() {
        return sync;
    }

    public void setSync(List<DaSyncConfig.SynchronizationInfo> sync) {
        this.sync = sync;
    }

    public DaSyncConfig.SynchronizationInfo getConfig(String code) {
        for(DaSyncConfig.SynchronizationInfo s: sync) {
            if (Objects.equals(s.code, code)) {
                return s;
            }
        }
        return null;
    }

    public List<DaSyncConfig.SynchronizationInfo> getConfig() {
        return sync;
    }
}
