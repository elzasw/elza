package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.repository.vo.DaoExternalSystemVO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DaoRepository extends ElzaJpaRepository<ArrDao, Integer> {

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId"
            + " and not exists (select dl from arr_dao_link dl where dl.dao = d and dl.deleteChange is null )")
    long countByDaoPackageIDAndNotExistsDaoLink(@Param("daoPackageId") Integer daoPackageId);

    @Query("select count(d) from arr_dao d "
            + " join d.daoPackage dp "
            + " where dp.daoPackageId = :daoPackageId")
    long countByDaoPackageID(@Param("daoPackageId") Integer daoPackageId);

    @Query("SELECT COUNT(d) > 0 FROM arr_dao d"
           + " JOIN arr_dao_link l ON l.daoId = d.daoId"
           + " WHERE l.nodeId = :nodeId AND d.daoType = 'LEVEL' AND l.deleteChangeId IS NULL")
    boolean existsDaoByNodeAndDaoTypeIsLevel(@Param("nodeId") Integer nodeId);

    ArrDao findOneByCode(String code);

    @Query("select d from arr_dao d where d.daoPackage = :daoPackage")
    List<ArrDao> findByPackage(@Param(value = "daoPackage") ArrDaoPackage arrDaoPackage);

    @Query("SELECT d FROM arr_dao d WHERE d.valid = TRUE AND d.daoPackage = :daoPackage")
    Page<ArrDao> findByPackage(ArrDaoPackage daoPackage, Pageable pageable);    

    @Modifying
    @Query("DELETE FROM arr_dao d WHERE d.daoPackageId IN (SELECT p.daoPackageId FROM arr_dao_package p WHERE p.fund = ?1)")
    void deleteByFund(ArrFund fund);

    @Query("SELECT d.dao.daoId FROM arr_dao_request_dao d JOIN d.daoRequest r WHERE d.dao IN (?1) AND r.state IN (?2)")
    List<Integer> findIdsByDaoIdsWhereArrRequestDaoExistInState(List<ArrDao> daos, List<ArrRequest.State> states);

    @Query("select d.daoId" +
            " from arr_dao d" +
            " where d.valid = true" +
            " and d.daoPackage.fund.fundId = :fundId")
    List<Integer> findValidIdByFund(@Param("fundId") Integer fundId);

    @Query("select new cz.tacr.elza.repository.vo.DaoExternalSystemVO(d.daoId, p.digitalRepository.externalSystemId)" +
            " from arr_dao d" +
            " join d.daoPackage p" +
            " where d.valid = true" +
            " and p.fund.fundId = :fundId" +
            " order by p.code")
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<DaoExternalSystemVO> findValidDaoExternalSystemByFund(@Param("fundId") Integer fundId);

    @Query("SELECT d FROM arr_dao d WHERE d.code in :codes")
    List<ArrDao> findByCodes(@Param(value = "codes") Collection<String> codes);

    @Query("SELECT d FROM arr_dao d" +
            " JOIN arr_dao_link dl ON dl.daoId = d.daoId" +
            " WHERE d.valid = TRUE" +
            "  AND dl.node = :node" +
            "  AND dl.deleteChange IS NULL" +
            " ORDER BY d.label ASC, d.code ASC")
    Page<ArrDao> findAttachedByNode(ArrNode node, Pageable pageable);

    @Query("SELECT d FROM arr_dao d" +
            " JOIN d.daoPackage p" +
            " WHERE d.valid = TRUE AND p.fund = :fund" +
            "  AND NOT EXISTS(SELECT dl FROM arr_dao_link dl" +
            "  JOIN dl.node n" +
            "  WHERE dl.dao = d" +
            "  AND dl.deleteChange IS NULL)" +
            " ORDER BY d.label ASC, d.code ASC")
    Page<ArrDao> findDettachedByFund(ArrFund fund, Pageable pageable);

    @Query("SELECT d FROM arr_dao d" +
            " JOIN d.daoPackage p" +
            " WHERE d.valid = TRUE AND p = :daoPackage" +
            "  AND NOT EXISTS(SELECT dl FROM arr_dao_link dl" +
            "  WHERE dl.dao = d" +
            "  AND dl.deleteChange IS NULL)" +
            " ORDER BY d.label ASC, d.code ASC")
    Page<ArrDao> findDettachedByPackage(ArrDaoPackage daoPackage, Pageable pageable);    
}
