package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.dataexchange.output.RefRecordsFromIds;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulItemType;

@Repository
public interface ApItemRepository extends JpaRepository<ApItem, Integer> {

    @Query("SELECT COUNT(i) FROM ApItem i WHERE i.itemType = ?1")
    long countByType(RulItemType dbItemType);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND i.part = :part")
    List<ApItem> findValidItemsByPart(@Param("part") ApPart part);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND i.part IN :parts")
    List<ApItem> findValidItemsByParts(@Param("parts") Collection<ApPart> parts);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND i.part.partId = :partId")
    List<ApItem> findValidItemsByPartId(@Param("partId") Integer partId);

    @Query("SELECT i FROM ApItem i JOIN FETCH i.data JOIN FETCH i.part p LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND p.accessPoint = :accessPoint")
    List<ApItem> findValidItemsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT i FROM ApItem i JOIN FETCH i.part p LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND p.accessPoint = :accessPoint AND i.createChange.changeId > :changeId")
    List<ApItem> findNewerValidItemsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint, @Param("changeId") Integer changeId);

    /**
     * Return items for given AccessPoints
     *
     * @param accessPoints
     * @return Return collection of ApItems for given access points.
     */
    @Query("SELECT i FROM ApItem i JOIN FETCH i.part p LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND p.accessPoint IN :accessPoints")
    List<ApItem> findValidItemsByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT i FROM ApItem i  JOIN FETCH i.part p JOIN FETCH i.itemType it JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND p.accessPoint = :accessPoint")
    List<ApItem> findValidItemsByAccessPointMultiFetch(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT i FROM ApItem i JOIN i.part p JOIN p.partType pt LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND i.itemType = :itemType AND p.deleteChange IS NULL AND pt.code = :partTypeCode AND p.accessPointId = :accessPointId")
    List<ApItem> findItemsByAccessPointIdAndItemTypeAndPartTypeCode(@Param("accessPointId") Integer accessPointId,
                                                                    @Param("itemType") RulItemType itemType,
                                                                    @Param("partTypeCode") String partTypeCode);

    @Query("SELECT i FROM ApItem i JOIN i.part p JOIN p.partType pt LEFT JOIN FETCH i.itemType it LEFT JOIN FETCH it.dataType WHERE i.deleteChange IS NULL AND i.itemType IN :itemTypes AND p.deleteChange IS NULL AND pt.code = :partTypeCode AND p.accessPointId = :accessPointId")
    List<ApItem> findItemsByAccessPointIdAndItemTypesAndPartTypeCode(@Param("accessPointId") Integer accessPointId,
                                                                     @Param("itemTypes") Collection<RulItemType> itemTypes,
                                                                     @Param("partTypeCode") String partTypeCode);

    @Query("SELECT part FROM ApItem i JOIN i.part part JOIN i.data data WHERE data IN :dataRecordRefList AND i.deleteChange IS NULL")
    List<ApPart> findPartsByDataRecordRefList(@Param("dataRecordRefList") List<ArrDataRecordRef> dataRecordRefList);

    @Query(value = "select count(*) from ap_item i" +
            " join ap_binding_item bi on bi.item_id = i.item_id and bi.delete_change_id is null" +
            " where i.delete_change_id is not null", nativeQuery = true)
    int countDeletedItemsWithUndeletedBindingItem();

    /**
     * Find usage of access point in another items
     *
     * Includes only valid access points (not deleted)
     *
     * @param replaced
     * @return
     */
    @Query("SELECT i FROM ApItem i JOIN FETCH i.part p "
            + "JOIN FETCH p.accessPoint "
            + "JOIN FETCH i.itemType it JOIN FETCH it.dataType JOIN arr_data_record_ref d ON i.data = d "
            + "JOIN ap_state st ON st.accessPointId = p.accessPointId AND st.deleteChangeId IS NULL "
            + "WHERE d.record = :record AND i.deleteChange IS NULL")
    List<ApItem> findItemByEntity(@Param("record") ApAccessPoint replaced);

    @Query("SELECT new cz.tacr.elza.dataexchange.output.RefRecordsFromIds(d.recordId, d.bindingId, p.accessPointId) FROM ApItem i "
           + "JOIN i.part p "
           + "JOIN i.data d "
           + "JOIN arr_data_record_ref ref ON ref.dataId = d.dataId "
           + "WHERE p.accessPointId IN :apIds AND p.deleteChange IS NULL AND i.deleteChange IS NULL")
    List<RefRecordsFromIds> findArrDataRecordRefRecordIdsByAccessPointIds(@Param("apIds") Collection<Integer> apIds);

    @Query("SELECT i FROM ApItem i JOIN FETCH i.part p JOIN FETCH p.parentPart JOIN FETCH p.accessPoint JOIN FETCH i.data JOIN arr_data_record_ref d ON i.data = d WHERE d.binding = :binding AND i.deleteChange IS NULL")
    List<ApItem> findUnbindedItemByBinding(@Param("binding") ApBinding binding);
}
