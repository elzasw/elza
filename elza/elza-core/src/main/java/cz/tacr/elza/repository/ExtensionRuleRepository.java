package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link RulExtensionRule}.
 *
 * @since 20.10.2017
 */
@Repository
public interface ExtensionRuleRepository extends JpaRepository<RulExtensionRule, Integer>, ExtensionRuleRepositoryCustom {

    List<RulExtensionRule> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    List<RulExtensionRule> findByRulPackageAndArrangementExtensionIn(RulPackage rulPackage, List<RulArrangementExtension> rulArrangementExtensions);

}
