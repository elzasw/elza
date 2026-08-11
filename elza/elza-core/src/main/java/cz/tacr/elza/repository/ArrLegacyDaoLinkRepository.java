package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrLegacyDaoLink;
import cz.tacr.elza.domain.ArrNode;

/**
 * Repository vazeb na {@link ArrDao} (legacy tvar). Vzniká výhradně
 * SOAP/WSDL tokem a připojováním DAO z balíčků; zaniká ve fázi 5 plánu
 * da-migration.md.
 */
@Repository
public interface ArrLegacyDaoLinkRepository extends JpaRepository<ArrLegacyDaoLink, Integer> {

    List<ArrLegacyDaoLink> findByDaoAndNodeAndDeleteChangeIsNull(ArrDao dao, ArrNode node);

    List<ArrLegacyDaoLink> findByDaoInAndDeleteChangeIsNull(Collection<ArrDao> page);

    List<ArrLegacyDaoLink> findByDaoAndDeleteChangeIsNull(ArrDao dao);

    List<ArrLegacyDaoLink> findByDao(ArrDao arrDao);

    @Query("SELECT dl" +
            " FROM arr_legacy_dao_link dl" +
            " JOIN FETCH dl.dao" +
            " WHERE dl.nodeId in :nodeIds" +
            " AND dl.deleteChange is null")
    List<ArrLegacyDaoLink> findByNodeIdsAndFetchDao(@Param(value = "nodeIds") Collection<Integer> nodeIds);

    @Query("SELECT dl FROM arr_legacy_dao_link dl JOIN FETCH dl.node node JOIN FETCH dl.dao WHERE node in :nodes AND dl.deleteChange IS NULL")
    List<ArrLegacyDaoLink> findByNodesAndFetchNodeAndDao(@Param(value = "nodes") Collection<ArrNode> nodes);

    @Query("SELECT dl" +
            " FROM arr_legacy_dao_link dl" +
            " JOIN FETCH dl.dao" +
            " WHERE dl.node = :node" +
            " AND dl.deleteChange is null")
    List<ArrLegacyDaoLink> findActiveByNode(@Param(value = "node") ArrNode node);

    @Query("SELECT dl" +
            " FROM arr_legacy_dao_link dl" +
            " JOIN FETCH dl.node" +
            " WHERE dl.dao in :daos" +
            " AND dl.deleteChange is null")
    List<ArrLegacyDaoLink> findActiveByDaos(@Param(value = "daos") Collection<ArrDao> daos);
}
