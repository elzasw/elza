package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFindingAidVersion;

public interface DataRecordRefRepositoryCustom {

    List<ArrDataRecordRef> fetchRecords(Set<Integer> recordRefDataIds, ArrFindingAidVersion version);
}
