package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.RulStructuredTypeExtension;

/**
 * Repozitory pro {@link ArrFundStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface FundStructureExtensionRepository extends JpaRepository<ArrFundStructureExtension, Integer> {

    ArrFundStructureExtension findByFundAndStructuredTypeExtensionAndDeleteChangeIsNull(ArrFund fund, RulStructuredTypeExtension structuredTypeExtension);

    List<ArrFundStructureExtension> findByFundAndDeleteChangeIsNull(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_fund_structure_extension se WHERE se.fund = ?1")
    void deleteByFund(ArrFund fund);
}
