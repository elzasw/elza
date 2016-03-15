package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataPartyRefRepositoryCustom {

    List<ArrDataPartyRef> findByDataIdsAndVersionFetchPartyRecord(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version);
}
