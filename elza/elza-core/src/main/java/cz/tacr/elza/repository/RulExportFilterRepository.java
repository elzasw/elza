package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulExportFilter;
import org.springframework.stereotype.Repository;

@Repository
public interface RulExportFilterRepository extends ElzaJpaRepository<RulExportFilter, Integer> {
}
