package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.DaDao;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;

/**
 * Repository for ArrDaoLink
 *
 * @since 1.9.2015
 */

@Repository
public interface DaoLinkRepository extends ElzaJpaRepository<ArrDaoLink, Integer>, DeleteFundHistory {

    List<ArrDaoLink> findByDaoAndNodeAndDeleteChangeIsNull(ArrDao dao, ArrNode node);

    List<ArrDaoLink> findByDaoInAndDeleteChangeIsNull(Collection<ArrDao> page);

    List<ArrDaoLink> findByDaoAndDeleteChangeIsNull(ArrDao dao);

    List<ArrDaoLink> findByDao(ArrDao arrDao);

    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.dao" +
            " WHERE dl.nodeId in :nodeIds" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findByNodeIdsAndFetchDao(@Param(value = "nodeIds") Collection<Integer> nodeIds);

    /**
     * Finds active links that reference an AIP (or a selected part of it) rather than a native
     * DAO. These links carry no {@link ArrDao}, so they are not part of the node cache. The AIP
     * with its repository and the optional {@link DaDao} part are fetched for export.
     */
    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.aip aip" +
            " JOIN FETCH aip.digitalRepository" +
            " LEFT JOIN FETCH dl.daDao" +
            " WHERE dl.nodeId in :nodeIds" +
            " AND dl.dao is null" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findAipLinksByNodeIdsAndFetchAip(@Param(value = "nodeIds") Collection<Integer> nodeIds);

    @Query("SELECT dl FROM arr_dao_link dl JOIN FETCH dl.node node JOIN FETCH dl.dao WHERE node in :nodes AND dl.deleteChange IS NULL")
    List<ArrDaoLink> findByNodesAndFetchNodeAndDao(@Param(value = "nodes") Collection<ArrNode> nodes);

    @Modifying
    void deleteByNode(ArrNode node);

    void deleteByNodeFund(ArrFund fund);

    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.dao" +
            " WHERE dl.node = :node" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findActiveByNode(@Param(value = "node") ArrNode node);

    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.node" +
            " WHERE dl.dao in :daos" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findActiveByDaos(@Param(value = "daos") Collection<ArrDao> daos);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(dl.daoLinkId, dl.createChangeId) FROM arr_dao_link dl "
            + "JOIN dl.node n "
            + "WHERE n.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_dao_link SET createChange = :change WHERE daoLinkId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);

    @Query("SELECT adl FROM arr_dao_link adl WHERE adl.aip.aipId = :aipId AND adl.deleteChange IS NULL")
    List<ArrDaoLink> findByAipIdAndDeleteChangeIsNull(@Param("aipId") Integer aipId);

    List<ArrDaoLink> findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(Integer aipId);

    List<ArrDaoLink> findByDaDaoInAndDeleteChangeIsNull(Collection<DaDao> daDaos);

    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.aip" +
            " WHERE dl.nodeId = :nodeId" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findByNodeIdAndDeleteChangeIsNullFetchAip(@Param("nodeId") Integer nodeId);

    /**
     * Returns codes of all ArrDao that are currently linked (any node, any fund)
     * within the given file-system digital repository. Codes are the repository-
     * relative paths stored on ArrDao. Used to mark browsed items as linked.
     * Global scope per §8 item 3 of fs-repo-analysis.md — no fund predicate.
     */
    @Query("SELECT DISTINCT dl.dao.code" +
            " FROM arr_dao_link dl" +
            " JOIN dl.dao.daoPackage p" +
            " WHERE p.digitalRepository = :digiRepo" +
            "   AND dl.dao IS NOT NULL" +
            "   AND dl.deleteChange IS NULL")
    List<String> findLinkedCodesByDigitalRepository(@Param("digiRepo") ArrDigitalRepository digiRepo);
}
