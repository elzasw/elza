package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoLinkRequest;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoLinkRequestRepository extends ElzaJpaRepository<ArrDaoLinkRequest, Integer> {

}
