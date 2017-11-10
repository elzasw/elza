package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link ArrStructureItem}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureItemRepository extends JpaRepository<ArrStructureItem, Integer> {

    List<ArrStructureItem> findByStructureDataAndDeleteChangeIsNull(ArrStructureData structureData);


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
}
