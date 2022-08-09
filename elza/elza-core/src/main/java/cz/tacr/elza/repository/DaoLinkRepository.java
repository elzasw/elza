package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
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

    List<ArrDaoLink> findByDaoAndDeleteChangeIsNull(ArrDao dao);

    List<ArrDaoLink> findByDao(ArrDao arrDao);

    @Query("SELECT dl" +
            " FROM arr_dao_link dl" +
            " JOIN FETCH dl.dao" +
            " WHERE dl.nodeId in :nodeIds" +
            " AND dl.deleteChange is null")
    List<ArrDaoLink> findByNodeIdsAndFetchDao(@Param(value = "nodeIds") Collection<Integer> nodeIds);

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
}
