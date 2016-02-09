package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataRecordRefRepositoryCustom {

    List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(Set<Integer> dataIds, Set<RulDescItemType> descItemTypes, ArrFindingAidVersion version);
}
