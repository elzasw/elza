package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructureData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozitory pro {@link ArrStructureData}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDataRepository extends JpaRepository<ArrStructureData, Integer>, StructureDataRepositoryCustom {

}
