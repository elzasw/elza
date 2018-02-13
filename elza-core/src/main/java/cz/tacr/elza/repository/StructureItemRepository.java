package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulItemType;

/**
 * Repozitory pro {@link ArrStructureItem}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureItemRepository extends JpaRepository<ArrStructureItem, Integer> {

    @Query("SELECT i FROM arr_structure_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structureDataId = :structureDataId")
    List<ArrStructureItem> findByStructureDataAndDeleteChangeIsNullFetchData(
            @Param("structureDataId") Integer structureDataId);

    @Query("SELECT i FROM arr_structure_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structureData = :structureData")
    List<ArrStructureItem> findByStructureDataAndDeleteChangeIsNullFetchData(
            @Param("structureData") ArrStructureData structureData);

    @Query("SELECT i FROM arr_structure_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.structureData IN :structureDataList")
    List<ArrStructureItem> findByStructureDataListAndDeleteChangeIsNullFetchData(@Param("structureDataList") List<ArrStructureData> structureDataList);

    @Query("SELECT i FROM arr_structure_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structureData = :structureData AND i.position > :position")
    List<ArrStructureItem> findOpenItemsAfterPosition(@Param("itemType") RulItemType itemType,
                                                      @Param("structureData") ArrStructureData structureData,
                                                      @Param("position") Integer position,
                                                      Pageable pageRequest);

    @Query("SELECT i FROM arr_structure_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structureData = :structureData AND i.position > :position")
    List<ArrStructureItem> findOpenItemsAfterPositionFetchData(@Param("itemType") RulItemType itemType,
                                                               @Param("structureData") ArrStructureData structureData,
                                                               @Param("position") Integer position,
                                                               Pageable pageRequest);

    @Query("SELECT i FROM arr_structure_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structureData = :structureData AND i.position >= :positionFrom AND i.position <= :positionTo")
    List<ArrStructureItem> findOpenItemsBetweenPositions(@Param("itemType") RulItemType itemType,
                                                         @Param("structureData") ArrStructureData structureData,
                                                         @Param("positionFrom") Integer positionFrom,
                                                         @Param("positionTo") Integer positionTo);

    @Query("SELECT i FROM arr_structure_item i JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.descItemObjectId = :itemObjectId")
    ArrStructureItem findOpenItemFetchData(@Param("itemObjectId") Integer itemObjectId);

    @Query("SELECT i FROM arr_structure_item i WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND i.structureData = :structureData")
    List<ArrStructureItem> findOpenItems(@Param("itemType") RulItemType itemType,
                                         @Param("structureData") ArrStructureData structureData);

    @Modifying
    @Query("DELETE FROM arr_structure_item i WHERE i.structureDataId IN (SELECT sd.structureDataId FROM arr_structure_data sd WHERE sd.state = 'TEMP')")
    void deleteByStructureDataStateTemp();

    @Modifying
    @Query("DELETE FROM arr_structure_item i WHERE i.structureData = :structureData")
    void deleteByStructureData(@Param("structureData") ArrStructureData structureData);

    @Query("SELECT COUNT(i) FROM arr_item i JOIN i.data d WHERE d.structureData = :structureData")
    Integer countItemsByStructureData(@Param("structureData") ArrStructureData structureData);

    @Modifying
    @Query("DELETE FROM arr_structure_item i WHERE i.structureData in (SELECT d FROM arr_structure_data d WHERE d.fund=?1)")
    void deleteByFund(ArrFund fund);
}
