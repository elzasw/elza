package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRequest;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface RequestRepository extends ElzaJpaRepository<ArrRequest, Integer>, RequestRepositoryCustom {

    ArrRequest findOneByCode(String code);
}
