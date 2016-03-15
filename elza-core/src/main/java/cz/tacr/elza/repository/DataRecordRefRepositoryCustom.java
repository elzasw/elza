package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataRecordRefRepositoryCustom {

    List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(Set<Integer> dataIds, Set<RulDescItemType> descItemTypes, ArrFundVersion version);
}
