package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemRepository extends ElzaJpaRepository<ArrDescItem, Integer>, DescItemRepositoryCustom {

    @Query("SELECT i FROM arr_desc_item i WHERE i.node in (?1) AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodesAndDeleteChangeIsNull(Collection<ArrNode> nodes);

    @Query("SELECT di FROM arr_desc_item di JOIN FETCH di.node WHERE di.node in (?1) AND di.deleteChange IS NULL AND di.itemType = ?2")
    List<ArrDescItem> findOpenByNodesAndType(Collection<ArrNode> nodes, RulItemType type);

    @Query("SELECT di FROM arr_desc_item di JOIN FETCH di.node WHERE di.node in (?1) AND di.deleteChange IS NULL AND di.itemType = ?2 AND di.itemSpec IN (?3)")
    List<ArrDescItem> findOpenByNodesAndTypeAndSpec(Collection<ArrNode> nodes, RulItemType type, Collection<RulItemSpec> specs);


    @Query("SELECT i FROM arr_desc_item i WHERE i.node in (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodesAndDeleteChange(Collection<ArrNode> nodes, ArrChange deleteChange);


    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNull(ArrNode node);

    @Query("SELECT i FROM arr_desc_item i WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodeIdsAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t JOIN i.itemSpec s WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.itemTypeId = ?2 AND s.itemSpecId = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeIdAndSpecItemTypeId(ArrNode node, Integer itemTypeId, Integer itemSpecId);

    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.itemTypeId = ?2")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeId(ArrNode node, Integer descItemTypeId);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeAndChange(ArrNode node, ArrChange change);

    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.node = ?1 AND t.itemTypeId = ?2 AND i.createChange < ?3 AND (i.deleteChange > ?3 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeItemTypeIdAndLockChangeId(ArrNode node, Integer itemTypeId, ArrChange change);


    /**
     * Najde otevřené atributy s daným nodem a type.
     * @param node nod uzlu
     * @param descItemTypes možné typy atributu
     * @return seznam atributů daného typu
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.itemType IN (?2)")
    List<ArrDescItem> findOpenByNodeAndTypes(ArrNode node, Set<RulItemType> descItemTypes);

    @Query("SELECT i FROM arr_desc_item i WHERE i.undefined = TRUE AND i.nodeId IN (?1) AND i.deleteChange IS NULL AND i.itemType IN (?2) AND (i.createChange < ?3 OR ?3 IS NULL) AND (i.deleteChange > ?3 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findUndefinedByNodeAndTypesAndChange(Collection<Integer> nodeIds, Collection<RulItemType> descItemTypes, ArrChange change);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    List<ArrDescItem> findOpenDescItems(Integer descItemObjectId);

    /**
     * Vyhledá otevřenou (nesmazenou) hodnotu atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return desc item
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    ArrDescItem findOpenDescItem(Integer descItemObjectId);


    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.itemType = ?1 AND i.node = ?2 AND i.position > ?3")
    List<ArrDescItem> findOpenDescItemsAfterPosition(RulItemType itemType, ArrNode node, Integer position);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.itemType = ?1 AND i.node = ?2")
    List<ArrDescItem> findOpenDescItems(RulItemType itemType, ArrNode node);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu mezi pozicemi. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.deleteChange IS NULL AND i.itemType = ?1 AND i.node = ?2 AND i.position >= ?3 AND i.position <= ?4")
    List<ArrDescItem> findOpenDescItemsBetweenPositions(RulItemType itemType, ArrNode node, Integer positionFrom, Integer positionTo);

    @Query("SELECT i FROM arr_desc_item i WHERE i.descItemObjectId = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByDescItemObjectIdAndLockChangeId(Integer descItemObjectId, ArrChange change);

    @Query("SELECT i FROM arr_desc_item i WHERE i.descItemObjectId = ?1 AND i.createChange >= ?2 AND (i.deleteChange >= ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByDescItemObjectIdAndBetweenVersionChangeId(Integer descItemObjectId, ArrChange change);

    /**
     * najde maximální pozici atributu archivního popisu podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné.
     * @param node zadaný nod
     * @param itemTypeId zadaný typ atribut archivního popisu.
     * @return maximální pozice
     */
    @Query(value = "SELECT max(i.position) FROM arr_desc_item i JOIN i.itemType t WHERE i.node = ?1 AND t.itemTypeId = ?2 AND i.deleteChange IS NULL")
    Integer findMaxPositionByNodeAndItemTypeIdAndDeleteChangeIsNull(ArrNode node, Integer itemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize je větší než zadaná pozice (position <= pozice).
     * @param position dolní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.position >= ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.itemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullAfterPosistion(Integer position, ArrNode node, Integer descItemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize je menší než zadaná pozice (pozice < position).
     * @param position horní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.position < ?1 AND i.node = ?2 AND i.deleteChange IS NULL AND t.itemTypeId = ?3")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBeforePosistion(Integer position, ArrNode node, Integer descItemTypeId);

    /**
     * najde záznamy podle nodu a typu atributu archivního popisu pokud je číslo smazání nevyplněné a pozize mezi zadanými (position < pozice <= position2).
     * @param position dolní pozice
     * @param position2 horní pozice
     * @param node zadaný nod
     * @param descItemTypeId zadaný typ atribut archivního popisu.
     * @return nalezené Atributy archivního popisu.
     */
    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.position > ?1 AND i.position <= ?2 AND i.node = ?3 AND i.deleteChange IS NULL AND t.itemTypeId = ?4")
    List<ArrDescItem> findByNodeAndDescItemTypeIdAndDeleteChangeIsNullBetweenPositions(Integer position, Integer position2, ArrNode node, Integer descItemTypeId);


    /**
     * najde záznamy podle nodu seřazený podle změny
     * @param node  zadaný nod
     * @return  nalezené atributy archivního popisu
     */
    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 order by i.createChange.changeDate asc")
    List<ArrDescItem> findByNodeOrderByCreateChangeAsc(ArrNode node);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.itemType = ?2 AND i.itemSpec = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeAndSpecItemType(ArrNode node, RulItemType itemType, RulItemSpec itemSpec);

    @Query("SELECT i FROM arr_desc_item i WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.itemType = ?2 AND i.itemSpec is null")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeAndSpecItemTypeIsNull(ArrNode node, RulItemType itemType);

    @Query("SELECT COUNT(i) FROM arr_desc_item i JOIN i.itemType t WHERE i.itemType = ?1")
    Long getCountByType(RulItemType itemType);


    @Query(value = "SELECT i FROM arr_desc_item i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.itemType it "
            + "left join fetch i.itemSpec dis "
            + "WHERE i.node = ?1 and i.deleteChange is null")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullFetch(ArrNode node);


}
