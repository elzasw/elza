package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemRepository extends JpaRepository<ArrDescItem, Integer> {

    List<ArrDescItem> findByNodeIdAndDeleteChangeIsNull(Integer nodeId);


    @Query("SELECT i FROM arr_desc_item i WHERE i.nodeId = ?1 "
            + "and i.createChange < ?2 and (i.deleteChange is null or i.deleteChange > ?2)")
    List<ArrDescItem> findByNodeId(Integer nodeId, ArrFaChange change);


    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t JOIN i.descItemSpec s WHERE i.nodeId = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2 AND s.descItemSpecId = ?3")
    List<ArrDescItem> findByNodeIdAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(Integer nodeId, Integer descItemTypeId, Integer descItemSpecId);


    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.nodeId = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2")
    List<ArrDescItem> findByNodeIdAndDeleteChangeIsNullAndDescItemTypeId(Integer nodeId, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    List<ArrDescItem> findByDescItemObjectIdAndDeleteChangeIsNull(Integer descItemObjectId);

    @Query(value = "SELECT max(i.descItemObjectId) FROM arr_desc_item i")
    Integer findMaxDescItemObjectId();

    @Query(value = "SELECT max(i.position) FROM arr_desc_item i JOIN i.descItemType t WHERE i.nodeId = ?1 AND t.descItemTypeId = ?2 AND i.deleteChange IS NULL")
    Integer findMaxPositionByNodeIdAndDescItemTypeIdAndDeleteChangeIsNull(Integer nodeId, Integer descItemTypeId);
}
