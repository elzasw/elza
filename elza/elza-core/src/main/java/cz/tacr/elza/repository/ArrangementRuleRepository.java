package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulArrangementRule.RuleType;

/**
 * Repository pro naimportovaný pravidla.
 *
 * @since 14.12.2015
 */
@Repository
public interface ArrangementRuleRepository extends JpaRepository<RulArrangementRule, Integer> {

    List<RulArrangementRule> findByRulPackage(RulPackage rulPackage);

    List<RulArrangementRule> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);

    List<RulArrangementRule> findByRuleSetIdAndRuleTypeOrderByPriority(Integer rulSetId, RuleType ruleType);

    void deleteByRulPackage(RulPackage rulPackage);

}
