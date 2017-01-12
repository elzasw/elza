package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Repository pro naimportovaný pravidla.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Repository
public interface RuleRepository extends JpaRepository<RulRule, Integer> {


    List<RulRule> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);


    List<RulRule> findByRuleSetAndRuleTypeOrderByPriorityAsc(RulRuleSet rulRuleSet,
                                                                     RulRule.RuleType attributeTypes);

}
