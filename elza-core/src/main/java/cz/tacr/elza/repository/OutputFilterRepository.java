package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutputFilterRepository extends ElzaJpaRepository<RulOutputFilter, Integer> {

    List<RulOutputFilter> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);
}
