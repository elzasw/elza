package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link ArrFundStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface FundStructureExtensionRepository extends JpaRepository<ArrFundStructureExtension, Integer> {

    ArrFundStructureExtension findByFundAndStructuredTypeExtensionAndDeleteChangeIsNull(ArrFund fund, RulStructuredTypeExtension structuredTypeExtension);

    List<ArrFundStructureExtension> findByFundAndDeleteChangeIsNull(ArrFund fund);
}
