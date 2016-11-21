package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulItemType;

public interface DataRecordRefRepositoryCustom {

    List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(Set<Integer> dataIds, Set<RulItemType> descItemTypes, ArrFundVersion version);

    List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(Set<Integer> dataIds, Set<RulItemType> itemTypes, Integer changeId);
}
