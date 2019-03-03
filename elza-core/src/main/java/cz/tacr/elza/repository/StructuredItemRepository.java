package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemType;

/**
 * Repozitory pro {@link ArrStructuredItem}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructuredItemRepository extends JpaRepository<ArrStructuredItem, Integer> {

    @Query("SELECT i FROM arr_structured_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structuredObjectId = :structuredObjectId")
    List<ArrStructuredItem> findByStructuredObjectAndDeleteChangeIsNullFetchData(
            @Param("structuredObjectId") Integer structuredObjectId);

    @Query("SELECT i FROM arr_structured_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structuredObject = :structuredObject")
    List<ArrStructuredItem> findByStructuredObjectAndDeleteChangeIsNullFetchData(
            @Param("structuredObject") ArrStructuredObject structuredObject);

    @Query("SELECT i FROM arr_structured_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structuredObject IN :structuredObjectList")
    List<ArrStructuredItem> findByStructuredObjectListAndDeleteChangeIsNullFetchData(@Param("structuredObjectList") List<ArrStructuredObject> structuredObjectList);

    @Query("SELECT i FROM arr_structured_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structuredObject = :structuredObject AND i.position > :position")
    List<ArrStructuredItem> findOpenItemsAfterPosition(@Param("itemType") RulItemType itemType,
                                                       @Param("structuredObject") ArrStructuredObject structuredObject,
                                                       @Param("position") Integer position,
                                                       Pageable pageRequest);

    @Query("SELECT i FROM arr_structured_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structuredObject = :structuredObject AND i.position > :position")
    List<ArrStructuredItem> findOpenItemsAfterPositionFetchData(@Param("itemType") RulItemType itemType,
                                                                @Param("structuredObject") ArrStructuredObject structuredObject,
                                                                @Param("position") Integer position,
                                                                Pageable pageRequest);

    @Query("SELECT i FROM arr_structured_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structuredObject = :structuredObject AND i.position >= :positionFrom AND i.position <= :positionTo")
    List<ArrStructuredItem> findOpenItemsBetweenPositions(@Param("itemType") RulItemType itemType,
                                                          @Param("structuredObject") ArrStructuredObject structuredObject,
                                                          @Param("positionFrom") Integer positionFrom,
                                                          @Param("positionTo") Integer positionTo);

    @Query("SELECT i FROM arr_structured_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.descItemObjectId = :itemObjectId")
    ArrStructuredItem findOpenItemFetchData(@Param("itemObjectId") Integer itemObjectId);

    @Query("SELECT i FROM arr_structured_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structuredObject = :structuredObject")
    List<ArrStructuredItem> findOpenItems(@Param("itemType") RulItemType itemType,
                                          @Param("structuredObject") ArrStructuredObject structuredObject);

    @Modifying
    @Query("DELETE FROM arr_structured_item i WHERE i.structuredObjectId IN (SELECT sd.structuredObjectId FROM arr_structured_object sd WHERE sd.state = 'TEMP')")
    void deleteByStructuredObjectStateTemp();

    @Modifying
    @Query("DELETE FROM arr_structured_item i WHERE i.structuredObject = :structuredObject")
    void deleteByStructuredObject(@Param("structuredObject") ArrStructuredObject structuredObject);

    int countItemsByStructuredObjectAndDeleteChangeIsNull(ArrStructuredObject structuredObject);

    @Query("SELECT COUNT(i) FROM arr_item i JOIN i.data d WHERE i.deleteChange IS NULL AND d.structuredObject = :structuredObject")
    Integer countItemsUsingStructObj(@Param("structuredObject") ArrStructuredObject structuredObject);

    @Modifying
    @Query("DELETE FROM arr_structured_item i WHERE i.structuredObject in (SELECT so FROM arr_structured_object so WHERE so.fund = ?1)")
    void deleteByStructuredObjectFund(ArrFund fund);

    /**
     * Count number of structured items within two changeIds
     * 
     * @param fundId
     * @param fromChange
     *            Higher change id
     * @param toChange
     *            Lower change id
     * @return
     */
    @Query("SELECT COUNT(i) FROM arr_structured_item i WHERE i.structuredObject.fundId = :fundId and " +
            "( " + 
            " (i.createChangeId>=:toChangeId AND i.createChangeId<=:fromChangeId) OR "+
            " (i.deleteChangeId is not null AND i.deleteChangeId>=:toChangeId AND i.deleteChangeId<=:fromChangeId) " +
            ")")
    int countItemsWithinChangeRange(@Param("fundId") Integer fundId,
                                    @Param("fromChangeId") Integer fromChange,
                                    @Param("toChangeId") Integer toChangeId);
}
