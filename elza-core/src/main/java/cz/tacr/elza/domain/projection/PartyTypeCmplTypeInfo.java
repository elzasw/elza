package cz.tacr.elza.domain.projection;

import org.springframework.beans.factory.annotation.Value;

public interface PartyTypeCmplTypeInfo {

    @Value("#{target.partyType.code}")
    String getPartyTypeCode();

    @Value("#{target.complementType.code}")
    String getComplementTypeCode();

    Boolean getRepeatable();
}
