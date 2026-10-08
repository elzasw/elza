package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutputFilterRepository extends ElzaJpaRepository<RulOutputFilter, Integer> {

    List<RulOutputFilter> findByRulPackage(RulPackage rulPackage);

    List<RulOutputFilter> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);

    void deleteByRulPackage(RulPackage rulPackage);

    RulOutputFilter findByRuleSetIdAndCode(Integer ruleSetId, String code);
}
