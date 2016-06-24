package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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


    @Query("SELECT i FROM arr_output_item i WHERE i.deleteChange IS NULL AND i.descItemObjectId = :itemObjectId")
    List<ArrOutputItem> findOpenOutputItems(@Param("itemObjectId") Integer descItemObjectId);

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
}
