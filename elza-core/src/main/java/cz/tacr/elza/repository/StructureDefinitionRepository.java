package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureDefinition}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDefinitionRepository extends JpaRepository<RulStructureDefinition, Integer>, Packaging<RulStructureType> {

    List<RulStructureDefinition> findByRulPackageAndStructureTypeIn(RulPackage rulPackage, List<RulStructureType> rulStructureTypes);

    List<RulStructureDefinition> findByStructureTypeAndDefTypeOrderByPriority(RulStructureType structureType, RulStructureDefinition.DefType defType);
}
