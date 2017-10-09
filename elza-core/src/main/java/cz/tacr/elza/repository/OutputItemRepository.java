package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;

@Repository
public interface OutputItemRepository extends JpaRepository<ArrOutputItem, Integer> {

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a výstupu. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param outputDefinition
     * @return
     */
    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.outputDefinition = :outputDefinition AND i.position > :position")
    List<ArrOutputItem> findOpenOutputItemsAfterPosition(@Param("itemType") RulItemType itemType,
                                                         @Param("outputDefinition") ArrOutputDefinition outputDefinition,
                                                         @Param("position") Integer position);

    /**
     * Vyhledá otevřenou (nesmazenou) hodnotu atributů podle objectId.
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @return output item
     */
    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = ?1")
    ArrOutputItem findOpenOutputItem(Integer descItemObjectId);

    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = :itemObjectId")
    List<ArrOutputItem> findOpenOutputItems(@Param("itemObjectId") Integer descItemObjectId);

    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.outputDefinition = :outputDefinition")
    List<ArrOutputItem> findOpenOutputItems(@Param("itemType") RulItemType itemType,
                                            @Param("outputDefinition") ArrOutputDefinition outputDefinition);

    @Query("SELECT i FROM arr_output_item i WHERE i.descItemObjectId = ?1 AND i.createChange >= ?2 AND (i.deleteChange >= ?2 OR i.deleteChange IS NULL)")
    List<ArrOutputItem> findByDescItemObjectIdAndBetweenVersionChangeId(Integer descItemObjectId, ArrChange change);

    @Query("SELECT i FROM arr_output_item i WHERE i.descItemObjectId = ?1 AND i.createChange < ?2 AND (i.deleteChange > ?2 OR i.deleteChange IS NULL)")
    List<ArrOutputItem> findByDescItemObjectIdAndLockChangeId(Integer descItemObjectId, ArrChange change);

    /**
     * Vyhledá všechny otevřené (nesmazené) hodnoty atributů podle typu a uzlu mezi pozicemi. (pro vícehodnotový atribut)
     *
     * @param itemType
     * @param node
     * @return
     */
    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.outputDefinition = :outputDefinition AND i.position >= :positionFrom AND i.position <= :positionTo")
    List<ArrOutputItem> findOpenOutputItemsBetweenPositions(@Param("itemType") RulItemType itemType,
                                                          @Param("outputDefinition") ArrOutputDefinition outputDefinition,
                                                          @Param("positionFrom") Integer positionFrom,
                                                          @Param("positionTo") Integer positionTo);

    @Modifying
    void deleteByOutputDefinition(ArrOutputDefinition outputDefinition);

    @Query("SELECT i FROM arr_output_item i WHERE i.outputDefinition = :outputDefinition AND i.deleteChange IS NULL")
    List<ArrOutputItem> findByOutputAndDeleteChangeIsNull(@Param("outputDefinition") ArrOutputDefinition outputDefinition);

    @Query("SELECT i FROM arr_output_item i WHERE i.outputDefinition = :outputDefinition AND i.createChange < :lockChange AND (i.deleteChange > :lockChange OR i.deleteChange IS NULL)")
    List<ArrOutputItem> findByOutputAndChange(@Param("outputDefinition") ArrOutputDefinition outputDefinition,
                                              @Param("lockChange") ArrChange lockChange);
}
