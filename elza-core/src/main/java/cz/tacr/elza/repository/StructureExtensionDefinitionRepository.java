package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureType;
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


    List<RulStructureExtensionDefinition> findByRulPackageAndStructureExtensionIn(RulPackage rulPackage, List<RulStructureExtension> rulStructureExtensionList);

    /**
     * Vyhledá aktivované definice rozšíření pro strukturovaný typ, def-type.
     *
     * @param structureType strukturovaný typ
     * @param defType       typ definice
     * @param fund          archivní soubor
     * @return nalezené soubory
     */
    @Query("SELECT sed FROM rul_structure_extension_definition sed JOIN sed.structureExtension se JOIN arr_fund_structure_extension fse ON se = fse.structureExtension WHERE se.structureType = :structureType AND sed.defType = :defType AND fse.deleteChange IS NULL AND fse.fund = :fund ORDER BY sed.priority")
    List<RulStructureExtensionDefinition> findByStructureTypeAndDefTypeAndFundOrderByPriority(@Param("structureType") RulStructureType structureType,
                                                                                              @Param("defType") RulStructureExtensionDefinition.DefType defType,
                                                                                              @Param("fund") ArrFund fund);
}
