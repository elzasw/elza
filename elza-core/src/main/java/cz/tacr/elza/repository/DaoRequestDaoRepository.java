package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoRequestDaoRepository extends ElzaJpaRepository<ArrDaoRequestDao, Integer>, DaoRequestDaoRepositoryCustom {

    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT dd.dao FROM arr_dao_request_dao dd WHERE dd.daoRequest = :daoRequest")
    List<ArrDao> findDaoByDaoRequest(@Param(value = "daoRequest") ArrDaoRequest arrDaoRequest);

    @Query("SELECT count(dd) FROM arr_dao_request_dao dd WHERE dd.daoRequest = :daoRequest")
    int countByDaoRequest(@Param(value = "daoRequest") ArrDaoRequest arrDaoRequest);

    List<ArrDaoRequestDao> findByDaoRequest(ArrDaoRequest arrDaoRequest);

    @Query("SELECT p FROM arr_dao_request_dao p WHERE p.daoRequest IN (?1)")
    List<ArrDaoRequestDao> findByDaoRequest(Collection<ArrDaoRequest> daoRequests);

    @Query("SELECT p FROM arr_dao_request_dao p WHERE p.daoRequest = ?1 AND p.dao IN (?2)")
    List<ArrDaoRequestDao> findByDaoRequestAndDao(ArrDaoRequest daoRequest, List<ArrDao> daos);

    List<ArrDaoRequestDao> findByDao(ArrDao arrDao);

    @Modifying
    void deleteByDaoRequest(ArrDaoRequest daoRequest);

    @Modifying
    @Query("DELETE FROM arr_dao_request_dao dd WHERE dd.daoRequestId IN (SELECT d.requestId FROM arr_dao_request d WHERE d.fund = ?1)")
    void deleteByFund(ArrFund fund);

    @Query("SELECT p FROM arr_dao_request_dao p JOIN p.daoRequest r WHERE p.dao IN (?1) AND r.state IN (?2)")
    List<ArrDaoRequestDao> findByDaoAndState(List<ArrDao> daos, List<ArrRequest.State> states);
}
