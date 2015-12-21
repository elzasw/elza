package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Respozitory pro pravidla.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface RuleSetRepository extends JpaRepository<RulRuleSet, Integer> {

    List<RulRuleSet> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);


    RulRuleSet findByCode(String ruleSetCode);
}
