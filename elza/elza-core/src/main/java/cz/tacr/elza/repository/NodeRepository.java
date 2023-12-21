package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrNodeConformity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.projection.NodeIdFundVersionIdInfo;

/**
 * Respozitory pro nody.
 */
@Repository
public interface NodeRepository extends ElzaJpaRepository<ArrNode, Integer>, NodeRepositoryCustom {

    @Query("SELECT distinct n.nodeId FROM arr_node n JOIN n.policies p JOIN n.levels l " +
            "WHERE l.deleteChange IS NULL AND n.fund = ?1")
    List<Integer> findNodeIdsForFondWithPolicy(ArrFund fund);

    /* Basic query:
     *
       select distinct n.node_id from arr_node n
       left join arr_level l on l.node_id = n.node_id
       where n.fund_id = ... and l.level_id is null
     */
    @Query("SELECT distinct n.nodeId FROM arr_node n " +
            "LEFT JOIN n.levels l " +
            "WHERE n.fund = ?1 AND l.levelId IS NULL")
    List<Integer> findUnusedNodeIdsByFund(ArrFund fund);

    /* Basic query:
    *
      select distinct n.node_id from arr_node n
      left join arr_level l on l.node_id = n.node_id
      where n.fund_id = ... and l.level_id is null
    */
    @Query("SELECT distinct n.nodeId FROM arr_node n " +
            "WHERE n.fund = ?1")
    Set<Integer> findNodeIdsByFund(ArrFund fund);

    @Query("SELECT distinct n.nodeId FROM arr_node n " +
            "LEFT JOIN n.levels l " +
            "WHERE l.levelId IS NULL")
    List<Integer> findUnusedNodeIds();

    ArrNode findOneByUuid(String uuid);

    List<ArrNode> findAllByUuidIn(Collection<String> uuids);

    List<ArrNode> findAllByNodeIdIn(Collection<Integer> nodeIds);

    @Modifying
    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    @Modifying
    @Query("DELETE FROM arr_node n WHERE n.fund = ?1")
    void deleteByFund(ArrFund fund);

    @Query("select distinct i.node" +
            " from arr_desc_item i" +
            " join arr_data_structure_ref dsr on dsr = i.data" +
            " where dsr.structuredObject.structuredObjectId in :structuredObjectIds" +
            " and i.deleteChange is null")
    List<ArrNode> findNodesByStructuredObjectIds(@Param(value = "structuredObjectIds") Collection<Integer> structuredObjectIds);

    @Query("SELECT node FROM arr_node node JOIN FETCH arr_node_conformity conf ON conf.node = node AND conf.state = :state")
    List<ArrNode> findNodesByConformityState(@Param(value= "state") ArrNodeConformity.State state);

    @Query("SELECT DISTINCT de.nodeId FROM arr_desc_item de " +
            "JOIN de.data d " +
            "JOIN arr_data_uri_ref ur ON ur.dataId = d.dataId " +
            "JOIN ur.arrNode n " +
            "WHERE de.deleteChange IS NULL AND n.nodeId = :nodeId")
    Set<Integer> findLinkedNodes(@Param("nodeId") Integer nodeId);
    
    /**
     * @return vrací seznam uzlů, které nemají žádnou vazbu na conformity info
     */
    @Query("SELECT n FROM arr_node n JOIN arr_level l ON l.node = n LEFT JOIN arr_node_conformity nc ON nc.node = n WHERE l.deleteChange IS NULL AND nc IS NULL")
    List<ArrNode> findByNodeConformityIsNull();

    /**
     * @return vrací seznam uzlů, které nemají žádnou vazbu na conformity info
     */
    @Query("SELECT n FROM arr_node n JOIN arr_level l ON l.node = n " +
            "LEFT JOIN arr_node_conformity nc ON nc.node = n " +
    		"WHERE n.fund = :fund AND l.deleteChange IS NULL AND nc IS NULL")
    List<ArrNode> findByNodeConformityIsNull(@Param(value= "fund") ArrFund fund);
    
    /**
     * @return vrací seznam dvojic nodeId a fundVersionId podle accessPointId
     */
    @Query("SELECT new cz.tacr.elza.domain.projection.NodeIdFundVersionIdInfo(d.nodeId, fv.fundVersionId) " + 
            "FROM arr_item i " +
            "JOIN arr_data_record_ref rf ON i.dataId = rf.dataId " +
            "JOIN arr_desc_item d ON i.itemId = d.itemId " +
            "JOIN arr_node n ON n.nodeId = d.nodeId " +
            "JOIN arr_level l ON n.nodeId = l.nodeId AND l.deleteChange is null " +
            "JOIN arr_fund_version fv ON n.fund = fv.fund AND fv.lockChange IS NULL " +
            "WHERE i.deleteChange IS NULL AND rf.recordId = :accessPointId")
    List<NodeIdFundVersionIdInfo> findNodeIdFundversionIdByAccessPointId(@Param(value = "accessPointId") Integer accessPointId);
}
