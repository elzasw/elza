package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApRuleRepository extends JpaRepository<ApRule, Integer> {

    List<ApRule> findByRuleSystemIn(Collection<ApRuleSystem> ruleSystems);

    @Query("SELECT r FROM ApRule r JOIN FETCH r.component c WHERE r.ruleSystem = :ruleSystem AND r.ruleType = :ruleType")
    ApRule findByRuleSystemAndRuleType(@Param("ruleSystem") ApRuleSystem ruleSystem, @Param("ruleType") ApRule.RuleType ruleType);
}
