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

    /**
     * Return collection of structured objects
     * 
     * @param nodeIds
     *            Collection of parent nodes
     * @param structuredTypeId
     *            Id structured, allow to limit result only to this type.
     *            If null StructuredObject of all types are returned
     * @param ignoreRootNodes
     *            Flag to include objects from parent nodes.
     *            If true object from parent nodes are not included.
     *            If false object from parent nodes are included in result.
     * @return
     */
    List<ArrStructuredObject> findStructureDataBySubtreeNodeIds(Collection<Integer> nodeIds, Integer structuredTypeId,
                                                                boolean ignoreRootNodes);
}
