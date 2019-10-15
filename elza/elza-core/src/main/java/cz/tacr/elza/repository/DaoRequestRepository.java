package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Repository
public interface DaoRequestRepository extends ElzaJpaRepository<ArrDaoRequest, Integer> {

    @Query("select r from arr_dao_request r where r.code = :code")
    List<ArrDaoRequest> findByCode(@Param(value = "code") String code);

    @Modifying
    void deleteByFund(ArrFund fund);
}
