package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoRequestDaoRepository extends ElzaJpaRepository<ArrDaoRequestDao, Integer> {

    List<ArrDaoRequestDao> findByDaoRequest(ArrDaoRequest arrDaoRequest);

    @Query("SELECT p FROM arr_dao_request_dao p WHERE p.daoRequest = ?1 AND p.dao IN (?2)")
    List<ArrDaoRequestDao> findByDaoRequestAndDao(ArrDaoRequest daoRequest, List<ArrDao> daos);

    List<ArrDaoRequestDao> findByDao(ArrDao arrDao);
}
