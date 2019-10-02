package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrRequest;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoLinkRequestRepository extends ElzaJpaRepository<ArrDaoLinkRequest, Integer> {

    @Query(value = "SELECT dlr FROM arr_dao_link_request dlr WHERE dlr.dao IN :daos AND dlr.state IN :states")
    List<ArrDaoLinkRequest> findByDaosAndStates(@Param(value = "daos") List<ArrDao> arrDaos, @Param(value = "states") List<ArrRequest.State> states);

    @Query("select r from arr_dao_link_request r where r.code = :code")
    List<ArrDaoLinkRequest> findByCode(@Param(value = "code") String code);

    List<ArrDaoLinkRequest> findByDao(ArrDao arrDao);

    @Modifying
    void deleteByFund(ArrFund fund);
}
