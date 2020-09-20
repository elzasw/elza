package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructuredType;

/**
 * Repozitory pro {@link RulStructureDefinition}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDefinitionRepository extends JpaRepository<RulStructureDefinition, Integer>, Packaging<RulStructureDefinition> {

    List<RulStructureDefinition> findByRulPackageAndStructuredTypeIn(RulPackage rulPackage, List<RulStructuredType> rulStructureTypes);

    // TODO: Replace structureType with structureTypeId
    @Query("select sd from rul_structure_definition sd JOIN FETCH sd.component c WHERE sd.structuredType=?1 ORDER BY sd.priority")
    List<RulStructureDefinition> findByStructTypeOrderByPriority(RulStructuredType structureType);
}
