package cz.tacr.elza.domain.projection;

import org.springframework.beans.factory.annotation.Value;

public interface ParPartyInfo {

	int getPartyId();

    @Value("#{target.record.recordId}")
    int getRecordId();

    int getVersion();
}
