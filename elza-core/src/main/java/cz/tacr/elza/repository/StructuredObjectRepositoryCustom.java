package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * Repozitory pro {@link ArrStructuredObject}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructuredObjectRepositoryCustom {

    FilteredResult<ArrStructuredObject> findStructureData(final Integer structuredTypeId, int fundId, String search, Boolean assignable, int firstResult, int maxResults);

    List<ArrStructuredObject> findStructureDataBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNodes);
}
