package cz.tacr.elza.domain.projection;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;

/**
 * Created by todtj on 30.05.2017.
 */
public interface RegRecordInfo {

    int getRecordId();

    @Value("#{target.scope.scopeId}")
    int getScopeId();

    LocalDateTime getLastUpdate();

    String getUuid();

    int getVersion();
}
