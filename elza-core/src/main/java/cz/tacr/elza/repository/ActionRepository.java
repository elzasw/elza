package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Repository pro nainportované hromadné akce.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Repository
public interface ActionRepository extends JpaRepository<RulAction, Integer> {


    List<RulAction> findByRulPackage(RulPackage rulPackage);

    List<RulAction> findByRuleSet(RulRuleSet ruleSet);

    List<RulAction> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);


    void deleteByRulPackage(RulPackage rulPackage);

    RulAction findOneByFilename(String name);

    List<RulAction> findByFilenameIn(List<String> names);

    @Query("SELECT DISTINCT r.action FROM rul_action_recommended r WHERE r.outputType = ?1")
    List<RulAction> findByRecommendedActionOutputType(RulOutputType outputType);
}
