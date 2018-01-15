package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.service.importnodes.vo.Structured;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

/**
 * Repozitory pro {@link ArrStructureData}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDataRepositoryCustom {

    FilteredResult<ArrStructureData> findStructureData(final Integer structureTypeId, int fundId, String search, Boolean assignable, int firstResult, int maxResults);

    Set<ArrStructureData> findStructureDataBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNodes);
}
