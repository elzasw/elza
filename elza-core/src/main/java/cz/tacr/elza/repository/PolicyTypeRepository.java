package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface PolicyTypeRepository extends JpaRepository<RulPolicyType, Integer> {

    List<RulPolicyType> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    RulPolicyType findByCode(String policyTypeCode);

    List<RulPolicyType> findByRuleSet(RulRuleSet ruleSet);

    @Query("SELECT t FROM rul_policy_type t WHERE t.policyTypeId in ?1")
    List<RulPolicyType> findByIds(Collection<Integer> policyTypeIds);
}
