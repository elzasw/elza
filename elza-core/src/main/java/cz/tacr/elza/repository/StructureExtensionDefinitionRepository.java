package cz.tacr.elza.repository;

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
     * Vyhledá definice rozšíření pro strukturovaný typ a def-type.
     *
     * @param structureType strukturovaný typ
     * @param defType       typ definice
     * @return nalezené soubory
     */
    @Query("SELECT sed FROM rul_structure_extension_definition sed JOIN sed.structureExtension se WHERE se.structureType = :structureType AND sed.defType = :defType ORDER BY sed.priority")
    List<RulStructureExtensionDefinition> findByStructureTypeAndDefTypeOrderByPriority(@Param("structureType") RulStructureType structureType,
                                                                                       @Param("defType") RulStructureExtensionDefinition.DefType defType);
}
