package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Vrací právě jedny pravidla pro fund podle poslední otevřené verze.
     *
     * @param fund archivní soubor
     * @return pravidla
     */
    @Query("SELECT rs FROM arr_fund_version fv JOIN fv.ruleSet rs WHERE fv.fund = :fund AND fv.lockChange IS NULL")
    RulRuleSet findByFund(@Param("fund") ArrFund fund);
}
