package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureDefinition}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDefinitionRepository extends JpaRepository<RulStructureDefinition, Integer>, Packaging<RulStructureDefinition> {

    List<RulStructureDefinition> findByRulPackageAndStructuredTypeIn(RulPackage rulPackage, List<RulStructuredType> rulStructureTypes);

    List<RulStructureDefinition> findByStructuredTypeAndDefTypeOrderByPriority(RulStructuredType structureType, RulStructureDefinition.DefType defType);
}
