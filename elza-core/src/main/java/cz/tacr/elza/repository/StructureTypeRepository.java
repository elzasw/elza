package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureType}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureTypeRepository extends JpaRepository<RulStructureType, Integer>, Packaging<RulStructureType> {

    List<RulStructureType> findByRuleSet(RulRuleSet ruleSet);

    List<RulStructureType> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet rulRuleSet);

    RulStructureType findByCode(String code);
}
