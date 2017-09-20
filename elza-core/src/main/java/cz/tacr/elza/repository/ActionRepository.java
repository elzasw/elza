package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulAction;


/**
 * Repository pro nainportované hromadné akce.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Repository
public interface ActionRepository extends JpaRepository<RulAction, Integer> {


    List<RulAction> findByRulPackage(RulPackage rulPackage);

    List<RulAction> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);


    void deleteByRulPackage(RulPackage rulPackage);

    RulAction findOneByFilename(String name);

    List<RulAction> findByFilenameIn(List<String> names);
}
