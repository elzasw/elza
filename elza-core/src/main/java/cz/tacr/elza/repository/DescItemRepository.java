package cz.tacr.elza.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemRepository extends JpaRepository<ArrDescItem, Integer> {

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t JOIN i.descItemSpec s WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2 AND s.descItemSpecId = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(ArrNode node, Integer descItemTypeId, Integer descItemSpecId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeId(ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND t.descItemTypeId = ?2 AND i.createChange < ?3 AND (i.deleteChange > ?3 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeDescItemTypeIdAndLockChangeId(ArrNode node, Integer descItemTypeId, ArrFaChange change);

    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    List<ArrDescItem> findByDescItemObjectIdAndDeleteChangeIsNull(Integer descItemObjectId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.descItemObjectId = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByDescItemObjectIdAndLockChangeId(Integer descItemObjectId, ArrFaChange change);

    @Query(value = "SELECT max(i.descItemObjectId) FROM arr_desc_item i")
    Integer findMaxDescItemObjectId();

    @Query(value = "SELECT max(i.position) FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND t.descItemTypeId = ?2 AND i.deleteChange IS NULL")
    Integer findMaxPositionByNodeAndDescItemTypeIdAndDeleteChangeIsNull(ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position >= ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.descItemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(Integer position, ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position < ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.descItemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(Integer position, ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position > ?1 AND i.position <= ?2 AND i.node = ?3 AND i.deleteChange IS NULL AND t.descItemTypeId = ?4")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(Integer position, Integer position2, ArrNode node, Integer descItemTypeId);

}
