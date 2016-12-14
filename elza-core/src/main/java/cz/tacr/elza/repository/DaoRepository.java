package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoRepository extends ElzaJpaRepository<ArrDao, Integer> {

    @Query("SELECT d FROM arr_dao d "
            + "join d.daoPackage p "
            + "join p.fund f "
            + "WHERE f.fundId = :fundId "
            + " and exists (select l from arr_dao_link l  join l.node n "
            + " where n.nodeId = :nodeId and l.dao.daoId = d.daoId and l.deleteChange IS NULL) ")
    public List<ArrDao> findByFundAndNode(@Param("fundId") Integer fundId, @Param("nodeId") Integer nodeId);

    @Query("SELECT d FROM arr_dao d "
            + "join d.daoPackage p "
            + "join p.fund f "
            + "WHERE f.fundId = :fundId "
            + " and not exists (select l from arr_dao_link l "
            + " where l.dao.daoId = d.daoId and l.deleteChange IS NULL) ")
    List<ArrDao> findByFundAndNotExistsNode(@Param("fundId") Integer fundId);

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId"
            + " and not exists (select dl from arr_dao_link dl where dl.dao = d and dl.deleteChange is null )")
    long countByDaoPackageIDAndNotExistsDaoLink(@Param("daoPackageId") Integer daoPackageId);

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId")
    long countByDaoPackageID(@Param("daoPackageId") Integer daoPackageId);
}
