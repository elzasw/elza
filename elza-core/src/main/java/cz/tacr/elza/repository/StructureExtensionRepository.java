package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface StructureExtensionRepository extends JpaRepository<RulStructureExtension, Integer>, Packaging<RulStructureExtension> {

    List<RulStructureExtension> findByRulPackageAndStructureTypeIn(RulPackage rulPackage, List<RulStructureType> rulStructureTypes);

    RulStructureExtension findByCode(String structureExtensionCode);

    List<RulStructureExtension> findByCodeIn(List<String> structureExtensionCodes);

    @Query("SELECT se FROM rul_structure_extension se JOIN se.structureType st JOIN st.ruleSet rs WHERE rs = :ruleSet")
    List<RulStructureExtension> findByRuleSet(@Param("ruleSet") RulRuleSet ruleSet);

    @Query("SELECT se FROM arr_fund_structure_extension fse JOIN fse.structureExtension se JOIN se.structureType st WHERE st.ruleSet = :ruleSet AND fse.fund = :fund AND fse.deleteChange IS NULL")
    List<RulStructureExtension> findActiveByFundAndRuleSet(@Param("fund") ArrFund fund,
                                                           @Param("ruleSet") RulRuleSet ruleSet);

}
