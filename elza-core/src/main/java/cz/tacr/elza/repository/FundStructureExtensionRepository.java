package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFundStructureExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozitory pro {@link ArrFundStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface FundStructureExtensionRepository extends JpaRepository<ArrFundStructureExtension, Integer> {

}
