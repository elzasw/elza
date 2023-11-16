package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.repository.vo.DaoExternalSystemVO;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DaoRepository extends ElzaJpaRepository<ArrDao, Integer> {

    @Query("SELECT COUNT(d) FROM arr_dao d "
            + " JOIN d.daoPackage dp "
            + " WHERE dp.daoPackageId = :daoPackageId"
            + " AND NOT EXISTS (SELECT dl FROM arr_dao_link dl WHERE dl.dao = d AND dl.deleteChange IS NULL)")
    long countByDaoPackageIDAndNotExistsDaoLink(@Param("daoPackageId") Integer daoPackageId);

    @Query("SELECT COUNT(d) FROM arr_dao d "
            + " JOIN d.daoPackage dp "
            + " WHERE dp.daoPackageId = :daoPackageId")
    long countByDaoPackageID(@Param("daoPackageId") Integer daoPackageId);

    @Query("SELECT COUNT(d) > 0 FROM arr_dao d"
           + " JOIN arr_dao_link l ON l.daoId = d.daoId"
           + " WHERE l.nodeId = :nodeId AND d.daoType = 'LEVEL' AND l.deleteChangeId IS NULL")
    boolean existsDaoByNodeAndDaoTypeIsLevel(@Param("nodeId") Integer nodeId);

    @Query("SELECT COUNT(d) > 0 FROM arr_dao d"
            + " JOIN arr_dao_link l ON l.daoId = d.daoId"
            + " WHERE l.node IN :nodes AND d.daoType = 'LEVEL' AND l.deleteChangeId IS NULL")
    boolean existsDaoByNodesAndDaoTypeIsLevel(@Param("nodes") Collection<ArrNode> nodes);

    ArrDao findOneByCode(String code);

    @Query("SELECT d FROM arr_dao d WHERE d.daoPackage = :daoPackage")
    List<ArrDao> findByPackage(@Param(value = "daoPackage") ArrDaoPackage arrDaoPackage);

    @Query("SELECT d FROM arr_dao d WHERE d.valid = TRUE AND d.daoPackage = :daoPackage")
    Page<ArrDao> findByPackagePageable(ArrDaoPackage daoPackage, Pageable pageable);

    @Modifying
    @Query("DELETE FROM arr_dao d WHERE d.daoPackageId IN (SELECT p.daoPackageId FROM arr_dao_package p WHERE p.fund = ?1)")
    void deleteByFund(ArrFund fund);

    @Query("SELECT d.dao.daoId FROM arr_dao_request_dao d JOIN d.daoRequest r WHERE d.dao IN (?1) AND r.state IN (?2)")
    List<Integer> findIdsByDaoIdsWhereArrRequestDaoExistInState(List<ArrDao> daos, List<ArrRequest.State> states);

    @Query("SELECT d.daoId" +
            " FROM arr_dao d" +
            " WHERE d.valid = true" +
            " AND d.daoPackage.fund.fundId = :fundId")
    List<Integer> findValidIdByFund(@Param("fundId") Integer fundId);

    @Query("SELECT new cz.tacr.elza.repository.vo.DaoExternalSystemVO(d.daoId, p.digitalRepository.externalSystemId)" +
            " FROM arr_dao d" +
            " JOIN d.daoPackage p" +
            " WHERE d.valid = true" +
            " AND p.fund.fundId = :fundId" +
            " ORDER BY p.code")
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<DaoExternalSystemVO> findValidDaoExternalSystemByFund(@Param("fundId") Integer fundId);

    @Query("SELECT d FROM arr_dao d WHERE d.code in :codes")
    List<ArrDao> findByCodes(@Param(value = "codes") Collection<String> codes);

    @Query("SELECT d FROM arr_dao d JOIN FETCH d.daoPackage p WHERE d.code IN :codes AND p.digitalRepository = :repo")
    List<ArrDao> findByCodes(@Param(value = "repo") ArrDigitalRepository repository,
                             @Param(value = "codes") List<String> daoCodes);

    @Query(value = "SELECT d FROM arr_dao d" +
            " JOIN FETCH d.daoPackage p" +
            " JOIN FETCH p.digitalRepository" +
            " JOIN arr_dao_link dl ON dl.daoId = d.daoId" +
            " WHERE d.valid = true" +
            "  AND dl.node = :node" +
            "  AND dl.deleteChange IS NULL" +
            " ORDER BY d.label ASC, d.code ASC", 
           countQuery = "SELECT COUNT(d) FROM arr_dao d" +
            " JOIN arr_dao_link dl ON dl.daoId = d.daoId" +
            " WHERE d.valid = true" +
            "  AND dl.node = :node" +
            "  AND dl.deleteChange IS NULL")
    Page<ArrDao> findAttachedByNode(@Param("node") ArrNode node, Pageable pageable);

    @Query("SELECT d FROM arr_dao d" +
            " JOIN d.daoPackage p" +
            " WHERE d.valid = true AND p.fund = :fund" +
            "  AND NOT EXISTS (SELECT dl FROM arr_dao_link dl" +
            "  JOIN dl.node n" +
            "  WHERE dl.dao = d" +
            "  AND dl.deleteChange IS NULL)" +
            " ORDER BY d.label ASC, d.code ASC")
    Page<ArrDao> findDettachedByFund(@Param("fund") ArrFund fund, Pageable pageable);

    @Query("SELECT d FROM arr_dao d" +
            " JOIN d.daoPackage p" +
            " WHERE d.valid = true AND p = :daoPackage" +
            "  AND NOT EXISTS (SELECT dl FROM arr_dao_link dl" +
            "  WHERE dl.dao = d" +
            "  AND dl.deleteChange IS NULL)" +
            " ORDER BY d.label ASC, d.code ASC")
    Page<ArrDao> findDettachedByPackage(@Param("daoPackage") ArrDaoPackage daoPackage, Pageable pageable);

    @Query("SELECT d FROM arr_dao d JOIN FETCH d.daoPackage p WHERE d.code IN :codes AND p.fund = :fund" +
            "  AND NOT EXISTS (SELECT dl FROM arr_dao_link dl" +
            "  WHERE dl.dao = d" +
            "  AND dl.deleteChange IS NULL)" +
            "AND p.digitalRepository = :repo")
    List<ArrDao> findDettachedByFundAndCodes(@Param(value = "repo") ArrDigitalRepository repository,
                                      @Param("fund") ArrFund fund,
                                      @Param(value = "codes") List<String> daoCodes);
}
