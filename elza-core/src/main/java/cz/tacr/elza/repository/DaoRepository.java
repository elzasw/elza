package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoRepository extends ElzaJpaRepository<ArrDao, Integer>, DaoRepositoryCustom {

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId"
            + " and not exists (select dl from arr_dao_link dl where dl.dao = d and dl.deleteChange is null )")
    long countByDaoPackageIDAndNotExistsDaoLink(@Param("daoPackageId") Integer daoPackageId);

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId")
    long countByDaoPackageID(@Param("daoPackageId") Integer daoPackageId);

    ArrDao findOneByCode(String code);

    @Query("select d from arr_dao d where d.daoPackage = :daoPackage")
    List<ArrDao> findByPackage(@Param(value = "daoPackage") ArrDaoPackage arrDaoPackage);

    @Modifying
    @Query("DELETE FROM arr_dao d WHERE d.daoPackageId IN (SELECT p.daoPackageId FROM arr_dao_package p WHERE p.fund = ?1)")
    void deleteByFund(ArrFund fund);

    @Query("SELECT d.dao.daoId FROM arr_dao_request_dao d JOIN d.daoRequest r WHERE d.dao IN (?1) AND r.state IN (?2)")
    List<Integer> findIdsByDaoIdsWhereArrRequestDaoExistInState(List<ArrDao> daos, List<ArrRequest.State> states);
}
