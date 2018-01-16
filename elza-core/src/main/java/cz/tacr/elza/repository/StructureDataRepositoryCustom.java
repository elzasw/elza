package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrStructureData;

/**
 * Repozitory pro {@link ArrStructureData}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDataRepositoryCustom {

    FilteredResult<ArrStructureData> findStructureData(final Integer structureTypeId, int fundId, String search, Boolean assignable, int firstResult, int maxResults);

    List<ArrStructureData> findStructureDataBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNodes);
}
