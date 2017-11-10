package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrStructureData;
import org.springframework.stereotype.Repository;

/**
 * Repozitory pro {@link ArrStructureData}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDataRepositoryCustom {

    FilteredResult<ArrStructureData> findStructureData(final Integer structureTypeId, int fundId, String search, boolean assignable, int firstResult, int maxResults);
}
