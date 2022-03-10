package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;

@Repository
public interface OutputItemRepository extends JpaRepository<ArrOutputItem, Integer> {

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a výstupu. (pro vícehodnotový atribut)
     */
    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.output = :output AND i.position > :position")
    List<ArrOutputItem> findOpenOutputItemsAfterPosition(@Param("itemType") RulItemType itemType,
                                                         @Param("output") ArrOutput output,
                                                         @Param("position") Integer position);

    /**
     * Vyhledá otevřenou (nesmazenou) hodnotu atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return output item
     */
    @Query("SELECT i FROM arr_output_item i JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    ArrOutputItem findOpenOutputItem(Integer descItemObjectId);

    /**
     * Return list of output items with fetched data
     */
    @Query("SELECT i FROM arr_output_item i LEFT JOIN FETCH i.data WHERE i.deleteChange IS NULL AND i.descItemObjectId = :itemObjectId")
    List<ArrOutputItem> findOpenOutputItems(@Param("itemObjectId") Integer descItemObjectId);

    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemTypeId = :itemTypeId AND i.output = :output")
    List<ArrOutputItem> findOpenOutputItemsByItemType(@Param("itemTypeId") Integer itemTypeId,
                                            @Param("output") ArrOutput output);

    /**
     * Finds maximum item position for specified type and definition. Only look up for open items.
     *
     * @return Maximum position or 0 when no item found.
     */
    @Query("SELECT COALESCE(MAX(i.position), 0) FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.output = :output")
    int findMaxItemPosition(@Param("itemType") RulItemType itemType,
                            @Param("output") ArrOutput output);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu mezi pozicemi. (pro vícehodnotový atribut)
     */
    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.output = :output AND i.position >= :positionFrom AND i.position <= :positionTo")
    List<ArrOutputItem> findOpenOutputItemsBetweenPositions(@Param("itemType") RulItemType itemType,
                                                            @Param("output") ArrOutput output,
                                                            @Param("positionFrom") Integer positionFrom,
                                                            @Param("positionTo") Integer positionTo);

    // @Modifying
    // void deleteByOutput(ArrOutput output);

    @Query("SELECT i FROM arr_output_item i LEFT JOIN FETCH i.data WHERE i.output = :output AND i.deleteChange IS NULL ORDER BY i.position")
    List<ArrOutputItem> findByOutputAndDeleteChangeIsNull(@Param("output") ArrOutput output);

    @Query("SELECT i FROM arr_output_item i LEFT JOIN FETCH i.data WHERE i.output = :output AND i.createChange < :change AND (i.deleteChange > :change OR i.deleteChange IS NULL) ORDER BY i.position")
    List<ArrOutputItem> findByOutputAndChange(@Param("output") ArrOutput output,
                                              @Param("change") ArrChange change);

    void deleteByOutputFund(ArrFund fund);
}
