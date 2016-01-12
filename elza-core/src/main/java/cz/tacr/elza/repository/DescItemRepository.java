package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemRepository extends JpaRepository<ArrDescItem, Integer>, DescItemRepositoryCustom {

    @Query("SELECT i FROM arr_desc_item i WHERE i.node in (?1) AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodesAndDeleteChangeIsNull(Collection<ArrNode> nodes);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node in (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodesAndDeleteChange(Collection<ArrNode> nodes, ArrChange deleteChange);


    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t JOIN i.descItemSpec s WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2 AND s.descItemSpecId = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(ArrNode node, Integer descItemTypeId, Integer descItemSpecId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.descItemTypeId = ?2")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeId(ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeAndChange(ArrNode node, ArrChange change);

    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND t.descItemTypeId = ?2 AND i.createChange < ?3 AND (i.deleteChange > ?3 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeDescItemTypeIdAndLockChangeId(ArrNode node, Integer descItemTypeId, ArrChange change);

    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    List<ArrDescItem> findByDescItemObjectIdAndDeleteChangeIsNull(Integer descItemObjectId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.descItemObjectId = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByDescItemObjectIdAndLockChangeId(Integer descItemObjectId, ArrChange change);

    @Query(value = "SELECT max(i.descItemObjectId) FROM arr_desc_item i")
    Integer findMaxDescItemObjectId();

    /**
     * najde maximální pozici atributu archivního popisu podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné.
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return maximální pozice
     */
    @Query(value = "SELECT max(i.position) FROM arr_desc_item i JOIN i.descItemType t WHERE i.node = ?1 AND t.descItemTypeId = ?2 AND i.deleteChange IS NULL")
    Integer findMaxPositionByNodeAndDescItemTypeIdAndDeleteChangeIsNull(ArrNode node, Integer descItemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize je větší než zadaná pozice (position <= pozice).
     * @param position dolní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position >= ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.descItemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(Integer position, ArrNode node, Integer descItemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize je menší než zadaná pozice (pozice < position).
     * @param position horní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position < ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.descItemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(Integer position, ArrNode node, Integer descItemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize mezi zadanými (position < pozice <= position2).
     * @param position dolní pozice
     * @param position2 horní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.descItemType t WHERE i.position > ?1 AND i.position <= ?2 AND i.node = ?3 AND i.deleteChange IS NULL AND t.descItemTypeId = ?4")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(Integer position, Integer position2, ArrNode node, Integer descItemTypeId);


    /**
     * najde záznamy podle nodu seřazený podle změny
     * @param node  zadaný nod
     * @return  nalezené atributy archivního popisu
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 order by i.createChange.changeDate asc")
    List<ArrDescItem> findByNodeOrderByCreateChangeAsc(ArrNode node);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.descItemType = ?2 AND i.descItemSpec = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemType(ArrNode node, RulDescItemType descItemType, RulDescItemSpec descItemSpec);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.descItemType = ?2 AND i.descItemSpec is null")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndDescItemTypeAndSpecItemTypeIsNull(ArrNode node, RulDescItemType descItemType);

}
