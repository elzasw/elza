package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repozitory pro {@link ArrFundStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface FundStructureExtensionRepository extends JpaRepository<ArrFundStructureExtension, Integer>, DeleteFundHistory {

    ArrFundStructureExtension findByFundAndStructuredTypeExtensionAndDeleteChangeIsNull(ArrFund fund, RulStructuredTypeExtension structuredTypeExtension);

    List<ArrFundStructureExtension> findByFundAndDeleteChangeIsNull(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_fund_structure_extension se WHERE se.fund = ?1")
    void deleteByFund(ArrFund fund);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(fse.fundStructureExtensionId, fse.createChange.changeId) FROM arr_fund_structure_extension fse "
            + "WHERE fse.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_fund_structure_extension SET createChange = :change WHERE fundStructureExtensionId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);
}
