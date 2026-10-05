package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.UsrPolicy;

/**
 * Repository for {@link UsrPolicy}.
 */
@Repository
public interface PolicyRepository extends ElzaJpaRepository<UsrPolicy, Integer> {

}
