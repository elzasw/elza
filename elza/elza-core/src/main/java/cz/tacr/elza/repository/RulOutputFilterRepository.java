package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputFilter;
import org.springframework.stereotype.Repository;

@Repository
public interface RulOutputFilterRepository extends ElzaJpaRepository<RulOutputFilter, Integer> {
}
