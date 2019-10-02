package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.RulStructuredType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructuredTypeExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface StructuredTypeExtensionRepository extends JpaRepository<RulStructuredTypeExtension, Integer>, Packaging<RulStructuredTypeExtension> {

    List<RulStructuredTypeExtension> findByRulPackageAndStructuredTypeIn(RulPackage rulPackage, List<RulStructuredType> rulStructureTypes);

    RulStructuredTypeExtension findByCode(String structureExtensionCode);

    List<RulStructuredTypeExtension> findByCodeIn(List<String> structureExtensionCodes);

    @Query("SELECT se FROM arr_fund_structure_extension fse JOIN fse.structuredTypeExtension se WHERE se.structuredType = :structuredType AND fse.fund = :fund AND fse.deleteChange IS NULL")
    List<RulStructuredTypeExtension> findActiveByFundAndStructureType(@Param("fund") ArrFund fund,
                                                                      @Param("structuredType") RulStructuredType structuredType);

    @Query("SELECT se FROM rul_structured_type_extension se WHERE se.structuredType = :structuredType")
    List<RulStructuredTypeExtension> findByStructureType(@Param("structuredType") RulStructuredType structuredType);
}
