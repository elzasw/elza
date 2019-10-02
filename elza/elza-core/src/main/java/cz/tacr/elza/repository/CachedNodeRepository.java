package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulItemType;


/**
 * Repository pro cachované JP.
 *
 */
@Repository
public interface CachedNodeRepository extends ElzaJpaRepository<ArrCachedNode, Integer> {

    ArrCachedNode findOneByNodeId(Integer nodeId);

	final static String FIND_BY_NODE_ID = "SELECT cd FROM arr_cached_node cd"
	        + " JOIN FETCH cd.node"
	        + " WHERE cd.nodeId IN (?1)";

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

    @Modifying
    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    @Modifying
    @Query("DELETE FROM arr_cached_node cn WHERE cn.nodeId IN (SELECT n.nodeId FROM arr_node n WHERE n.fund = ?1)")
	void deleteByFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_cached_node cn WHERE cn.nodeId IN (SELECT di.nodeId FROM arr_desc_item di WHERE di.itemType = ?1)")
    int deleteByItemType(RulItemType itemType);
}
