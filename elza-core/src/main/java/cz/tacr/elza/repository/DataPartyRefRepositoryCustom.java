package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataPartyRefRepositoryCustom {

    List<ArrDataPartyRef> findByDataIdsAndVersionFetchPartyRecord(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFindingAidVersion version);
}
