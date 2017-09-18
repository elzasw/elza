package cz.tacr.elza.domain.projection;

import org.springframework.beans.factory.annotation.Value;

/**
 * Created by todtj on 30.05.2017.
 */
public interface RegRecordInfoExternal extends RegRecordInfo {

    String getExternalId();

    @Value("#{target.externalSystem.code}")
    String getExternalSystemCode();
}
