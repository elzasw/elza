package cz.tacr.elza.domain.projection;

import org.springframework.beans.factory.annotation.Value;

public interface ApAccessPointInfoExternal extends ApAccessPointInfo {

    String getExternalId();

    @Value("#{target.externalSystem.code}")
    String getExternalSystemCode();
}
