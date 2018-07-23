package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApFragmentRule;
import cz.tacr.elza.domain.ApFragmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApFragmentRuleRepository extends JpaRepository<ApFragmentRule, Integer> {

    List<ApFragmentRule> findByFragmentTypeIn(Collection<ApFragmentType> types);

    @Query("SELECT fr FROM ApFragmentRule fr JOIN FETCH fr.component c WHERE fr.fragmentType = :fragmentType AND fr.ruleType = :ruleType")
    ApFragmentRule findByFragmentTypeAndRuleType(@Param("fragmentType") ApFragmentType fragmentType,
                                                 @Param("ruleType") ApFragmentRule.RuleType ruleType);
}
