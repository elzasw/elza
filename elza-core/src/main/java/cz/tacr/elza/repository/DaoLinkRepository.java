package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;

/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */

@Repository
public interface DaoLinkRepository extends ElzaJpaRepository<ArrDaoLink, Integer> {

    List<ArrDaoLink> findByDaoAndNodeAndDeleteChangeIsNull(ArrDao dao, ArrNode node);

    List<ArrDaoLink> findByDaoAndDeleteChangeIsNull(ArrDao dao);

    List<ArrDaoLink> findByDao(ArrDao arrDao);

    List<ArrDaoLink> findByNodeIdInAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Modifying
    void deleteByNode(ArrNode node);

    void deleteByNodeFund(ArrFund fund);

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
}
