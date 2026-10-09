package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportFilterRepository extends ElzaJpaRepository<RulExportFilter, Integer> {

    List<RulExportFilter> findByRulPackage(RulPackage rulPackage);

    List<RulExportFilter> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet ruleSet);

	RulExportFilter findByCode(String code);

	void deleteByRulPackage(RulPackage rulPackage);
}
