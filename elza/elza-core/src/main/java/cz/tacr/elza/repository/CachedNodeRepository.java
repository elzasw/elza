package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;


/**
 * Repository pro cachované JP.
 *
 */
@Repository
public interface CachedNodeRepository extends ElzaJpaRepository<ArrCachedNode, Integer> {

	final static String FIND_BY_NODE_ID = "SELECT cd FROM arr_cached_node cd JOIN FETCH cd.node WHERE cd.nodeId IN (?1)";

	/**
	 * Fetch list of nodes from cache
	 *
	 * Method will fetch ArrCachedNode and ArrNode
	 *
	 * @param nodeIds
	 * @return
	 */
	@Query(FIND_BY_NODE_ID)
	List<ArrCachedNode> findByNodeIdIn(Collection<Integer> nodeIds);

	@Query(FIND_BY_NODE_ID)
	ArrCachedNode findByNodeId(Integer nodeId);

    @Query("SELECT cn FROM arr_cached_node cn WHERE cn.nodeId IN (?1)")
    List<ArrCachedNode> findByNodeIdsInNoFetch(Collection<Integer> nodeIds);

	@Query("SELECT cn FROM arr_cached_node cn JOIN FETCH cn.node node WHERE node IN (?1)")
    List<ArrCachedNode> findByNodeIn(Collection<ArrNode> nodes);

    @Modifying
    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    @Modifying
    @Query("DELETE FROM arr_cached_node cn WHERE cn.nodeId IN (SELECT n.nodeId FROM arr_node n WHERE n.fund = ?1)")
	void deleteByFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_cached_node cn WHERE cn.nodeId IN (SELECT di.nodeId FROM arr_desc_item di WHERE di.itemType = ?1)")
    int deleteByItemType(RulItemType itemType);

    @Query("SELECT cn FROM arr_cached_node cn WHERE cn.nodeId IN (SELECT adl.nodeId FROM arr_dao_link adl WHERE adl.aip.aipId = ?1)")
    List<ArrCachedNode> findByConnectedToAipId(Integer aipId);

    /**
     * Returns cache row node IDs for nodes whose every {@code arr_level} row
     * has {@code deleteChange IS NOT NULL} — i.e. invalid nodes that violate
     * the row-existence invariant of the cache. Used by startup cleanup to
     * drop stale entries from the cache and Lucene index.
     */
    @Query("SELECT cn.nodeId FROM arr_cached_node cn " +
           "WHERE NOT EXISTS (SELECT 1 FROM arr_level l " +
           "                  WHERE l.node.nodeId = cn.nodeId AND l.deleteChange IS NULL)")
    List<Integer> findInvalidCachedNodeIds();
}
