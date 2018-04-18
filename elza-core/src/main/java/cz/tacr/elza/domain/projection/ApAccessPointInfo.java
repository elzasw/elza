package cz.tacr.elza.domain.projection;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

public interface ApAccessPointInfo {

    int getRecordId();

    @Value("#{target.scope.scopeId}")
    int getScopeId();

    LocalDateTime getLastUpdate();

    String getUuid();

    int getVersion();
}
