package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureExtensionDefinition}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureExtensionDefinitionRepository extends JpaRepository<RulStructureExtensionDefinition, Integer>, Packaging<RulStructureExtensionDefinition> {


    List<RulStructureExtensionDefinition> findByRulPackageAndStructuredTypeExtensionIn(RulPackage rulPackage, List<RulStructuredTypeExtension> rulStructureExtensionList);

    /**
     * Vyhledá aktivované definice rozšíření pro strukturovaný typ, def-type.
     *
     * @param structuredType strukturovaný typ
     * @param defType       typ definice
     * @param fund          archivní soubor
     * @return nalezené soubory
     */
    @Query("SELECT sed FROM rul_structure_extension_definition sed JOIN sed.structuredTypeExtension se JOIN arr_fund_structure_extension fse ON se = fse.structuredTypeExtension WHERE se.structuredType = :structuredType AND sed.defType = :defType AND fse.deleteChange IS NULL AND fse.fund = :fund ORDER BY sed.priority")
    List<RulStructureExtensionDefinition> findByStructureTypeAndDefTypeAndFundOrderByPriority(@Param("structuredType") RulStructuredType structuredType,
                                                                                              @Param("defType") RulStructureExtensionDefinition.DefType defType,
                                                                                              @Param("fund") ArrFund fund);

    /**
     * Vyhledá aktivované definice rozšíření pro strukturovaný typ, def-type.
     *
     * @param structuredType strukturovaný typ
     * @param defType       typ definice
     * @return nalezené soubory
     */
    @Query("SELECT sed FROM rul_structure_extension_definition sed JOIN sed.structuredTypeExtension se WHERE se.structuredType = :structuredType AND sed.defType = :defType ORDER BY sed.priority")
    List<RulStructureExtensionDefinition> findByStructureTypeAndDefTypeOrderByPriority(@Param("structuredType") RulStructuredType structuredType,
                                                                                       @Param("defType") RulStructureExtensionDefinition.DefType defType);
}
