package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * Repository for Description items
 *
 * Some methods are also fetching related entities like node and data. Please
 * consider which method is suitable for each use.
 */
@Repository
public interface DescItemRepository extends ElzaJpaRepository<ArrDescItem, Integer>, DescItemRepositoryCustom {

	@Query("SELECT di FROM arr_desc_item di LEFT JOIN FETCH di.data JOIN FETCH di.node WHERE di.node in (?1) AND di.deleteChange IS NULL AND di.itemType = ?2")
    List<ArrDescItem> findOpenByNodesAndType(Collection<ArrNode> nodes, RulItemType type);

	@Query("SELECT di FROM arr_desc_item di LEFT JOIN FETCH di.data JOIN FETCH di.node WHERE di.node in (?1) AND di.deleteChange IS NULL AND di.itemType = ?2 AND di.itemSpec IN (?3)")
    List<ArrDescItem> findOpenByNodesAndTypeAndSpec(Collection<ArrNode> nodes, RulItemType type, Collection<RulItemSpec> specs);

	@Query("SELECT di FROM arr_desc_item di LEFT JOIN FETCH di.data JOIN FETCH di.node n WHERE n.fund = :fund AND di.deleteChange IS NULL AND di.itemType = :type")
    List<ArrDescItem> findOpenByFundAndType(@Param("fund") ArrFund fund,
                                            @Param("type") RulItemType type);

	@Query("SELECT di FROM arr_desc_item di LEFT JOIN FETCH di.data JOIN FETCH di.node n WHERE n.fund = :fund AND di.deleteChange IS NULL AND di.itemType = :type AND di.itemSpec IN :specs")
    List<ArrDescItem> findOpenByFundAndTypeAndSpec(@Param("fund") ArrFund fund,
                                                   @Param("type") RulItemType type,
                                                   @Param("specs") Collection<RulItemSpec> specs);

	@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data JOIN FETCH i.node n WHERE i.node in (?1) AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodesAndDeleteChange(Collection<ArrNode> nodes, ArrChange deleteChange);


	//TODO: Consider to remove this method
    @Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.node = ?1 AND i.deleteChange IS NULL")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNull(ArrNode node);

	/*static final String FETCH_NODES_WITH_DATA = "SELECT i, d, dp, par FROM arr_desc_item i"
	        + " LEFT JOIN FETCH i.data d"
	        + " LEFT JOIN arr_data_party_ref dp ON dp.dataId = d.dataId"
	        + " LEFT JOIN FETCH dp.party par"
	        + " WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL";*/

	static final String FETCH_NODES_WITH_DATA = "SELECT i FROM arr_desc_item i"
	        + " LEFT JOIN FETCH i.data d"
            + " WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL"
            + " ORDER BY i.nodeId, i.itemTypeId, i.itemSpecId, i.position";

    /**
     * Read node and connected data
     *
     * Items are ordered by: node, itemType, itemSpec, position
     *
     * @param nodeIds
     * @return
     */
	//@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data d WHERE i.nodeId IN (?1) AND i.deleteChange IS NULL")
	@Query(FETCH_NODES_WITH_DATA)
    List<ArrDescItem> findByNodeIdsAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t JOIN i.itemSpec s WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.itemTypeId = ?2 AND s.itemSpecId = ?3")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeIdAndSpecItemTypeId(ArrNode node, Integer itemTypeId, Integer itemSpecId);

	/**
	 * Read single description item and connected data
	 *
	 * @param node
	 * @param descItemTypeId
	 * @return Return description item and fetched data
	 */
	@Query("SELECT i FROM arr_desc_item i JOIN i.itemType t LEFT JOIN FETCH i.data WHERE i.node = ?1 AND i.deleteChange IS NULL AND t.itemTypeId = ?2")
    List<ArrDescItem> findByNodeAndDeleteChangeIsNullAndItemTypeId(ArrNode node, Integer descItemTypeId);

	/**
	 * Return list of description items for node.
	 *
	 * Function fetch description items, data and item type
	 *
	 * @param node
	 *            Node for which data are fetched. Cannot be null.
	 * @param change
	 *            Change to use for fetching data. Cannot be null.
	 * @return
	 */
	@Query("SELECT i FROM arr_desc_item i JOIN FETCH i.itemType t LEFT JOIN FETCH i.data WHERE i.node = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
	List<ArrDescItem> findByNodeAndChange(ArrNode node, ArrChange change);

    @Query("SELECT i FROM arr_desc_item i JOIN i.itemType t WHERE i.node = ?1 AND t.itemTypeId = ?2 AND i.createChange < ?3 AND (i.deleteChange > ?3 OR i.deleteChange IS NULL)")
    List<ArrDescItem> findByNodeItemTypeIdAndLockChangeId(ArrNode node, Integer itemTypeId, ArrChange change);


    /**
	 * Najde otevřené atributy s daným nodem a type a načte je včetně hodnot
	 *
	 * @param node
	 *            nod uzlu
	 * @param descItemTypes
	 *            možné typy atributu
	 * @return seznam atributů daného typu
	 */
    @Query("SELECT i FROM arr_desc_item i " +
            "LEFT JOIN FETCH i.data " +
            "WHERE i.node = ?1 AND i.deleteChange IS NULL AND i.itemType IN (?2) " +
            "ORDER BY i.position")
    List<ArrDescItem> findOpenByNodeAndTypes(ArrNode node, Set<RulItemType> descItemTypes);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return
     */
	@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    List<ArrDescItem> findOpenDescItems(Integer descItemObjectId);

    @Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.descItemObjectId IN :descItemObjectIds")
    List<ArrDescItem> findOpenDescItemsByIds(@Param("descItemObjectIds") Collection<Integer> descItemObjectIds);

    /**
     * Vyhledá otevřenou (nesmazenou) hodnotu atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return desc item
     */
	@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    ArrDescItem findOpenDescItem(Integer descItemObjectId);


    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
	@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.itemType = ?1 AND i.node = ?2 AND i.position > ?3")
    List<ArrDescItem> findOpenDescItemsAfterPosition(RulItemType itemType, ArrNode node, Integer position);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
	@Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.itemType = ?1 AND i.node = ?2")
    List<ArrDescItem> findOpenDescItemsByItemType(RulItemType itemType, ArrNode node);

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

    @Query("SELECT n.fundId, i.nodeId, d.dataId FROM arr_desc_item i JOIN i.data d JOIN i.node n WHERE i.deleteChange IS NULL and i.data in (?1)")
    List<Object[]> findFundIdNodeIdDataIdByDataAndDeleteChangeIsNull(List<? extends ArrData> data);

    @Query("Select i from arr_desc_item i join arr_data_record_ref d on i.data = d WHERE d.record = :record AND i.deleteChange IS NULL")
    List<ArrDescItem> findArrItemByRecord(@Param("record") final ApAccessPoint record);

    @Query("SELECT i.id FROM arr_desc_item i WHERE i.node = :node AND i.createChange >= :change")
    List<Integer> findIdByNodeAndCreatedAfterChange(@Param("node") final ArrNode node, @Param("change") final ArrChange change);

    @Query("SELECT i.id FROM arr_desc_item i WHERE i.node.fund = :fund AND i.createChange >= :change")
    List<Integer> findIdByFundAndCreatedAfterChange(@Param("fund") final ArrFund fund, @Param("change") final ArrChange change);

    @Query("SELECT i.id FROM arr_desc_item i WHERE i.node = :node AND i.deleteChange IS NULL")
    List<Integer> findOpenIdByNodeAndCreatedAfterChange(@Param("node") final ArrNode node);

    @Query("SELECT i.id FROM arr_desc_item i WHERE i.node.fund = :fund AND i.deleteChange IS NULL")
    List<Integer> findOpenIdByFundAndCreatedAfterChange(@Param("fund") final ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_desc_item di WHERE di.node IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByNodeFund(ArrFund fund);

    @Modifying
    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    /**
     * Dotaz vyhleda items navazane na dany uzel prostrednictvim ArrDataUriRef
     * 
     * Vraci plne nactena data ArrDataUriRef.
     * 
     * Dotaz je nyni optimalizovan na rychlost na PostgreSQL. Problemem
     * je zajistit, aby se cast "join fetch" vykonavala az po aplikaci
     * filtru dle node.
     * 
     * @param node
     * @return
     */
    @Query("SELECT i FROM arr_desc_item i JOIN FETCH i.data where i in (SELECT i FROM arr_desc_item i JOIN ArrDataUriRef d on i.data=d WHERE d.arrNode = :node AND i.deleteChange IS NULL)")
    List<ArrDescItem> findByUriDataNode(@Param("node") final ArrNode node);

    /**
     * Získání platných itemů mezi pozicema.
     *
     * @param itemType pro konktrétní typ atributu
     * @param node     pro kontkrétní JP
     * @return nalezené hodnoty atributů
     */
    @Query("SELECT i FROM arr_desc_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.node = :node AND i.position >= :positionFrom AND i.position <= :positionTo")
    List<ArrDescItem> findOpenDescItemsBetweenPositions(@Param("itemType") RulItemType itemType,
                                                        @Param("node") ArrNode node,
                                                        @Param("positionFrom") int positionFrom,
                                                        @Param("positionTo") int positionTo);


}
