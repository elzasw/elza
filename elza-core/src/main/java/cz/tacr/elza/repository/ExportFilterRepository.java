package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportFilterRepository extends ElzaJpaRepository<RulExportFilter, Integer> {

    List<RulExportFilter> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);
}
