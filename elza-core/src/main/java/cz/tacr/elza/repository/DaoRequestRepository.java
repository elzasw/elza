package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoRequest;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoRequestRepository extends ElzaJpaRepository<ArrDaoRequest, Integer> {

}
