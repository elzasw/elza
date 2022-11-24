package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

/**
 * Repository záznamy v rejstříku.
 */
@Repository
public interface ApAccessPointRepository extends ElzaJpaRepository<ApAccessPoint, Integer>, ApAccessPointRepositoryCustom {

    ApAccessPoint findAccessPointByUuid(String uuid);

    @Query("SELECT a.accessPointId FROM ap_access_point a WHERE a.state = :state")
    List<Integer> findAccessPointIdByState(@Param("state") ApStateEnum state);

    @Query("SELECT ap FROM ap_access_point ap WHERE ap.uuid IN :uuids")
    List<ApAccessPoint> findApAccessPointsByUuids(@Param("uuids") Collection<String> uuids);

    /**
     * Searches not deleted APs by UUIDs.
     *
     * @return AP projection
     */
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT new cz.tacr.elza.domain.projection.ApAccessPointInfo(ap.accessPointId, ap.uuid, s.stateId, s.scopeId, s.apTypeId)" +
            " FROM ap_state s" +
            " JOIN s.accessPoint ap" +
            " WHERE s.accessPoint.uuid IN (:uuids)" +
            " AND s.createChangeId in (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s.accessPoint)")
    List<ApAccessPointInfo> findActiveInfoByUuids(@Param("uuids") Collection<String> uuids);

    @Query("SELECT distinct i.itemId" +
            " FROM arr_item i" +
            " JOIN arr_data_record_ref rr ON (rr.dataId = i.dataId)" +
            " WHERE rr.record.accessPointId = ?1")
    List<Integer> findItemIdByAccessPointIdOverDataRecordRef(Integer accessPointId);

    @Query("SELECT ap.accessPointId FROM ap_access_point ap " +
            "JOIN ApPart part ON ap.accessPointId = part.accessPoint.accessPointId " +
            "JOIN ApItem item ON part.partId = item.part.partId " +
            "WHERE item.deleteChange IS NULL AND item.data.dataId IN :dataIds")
    List<Integer> findAccessPointIdsByRefDataId(@Param("dataIds") Collection<Integer> dataIds);

    @Query("SELECT ap.accessPointId FROM ap_access_point ap " +
            "JOIN ApPart part ON ap.accessPointId = part.accessPoint.accessPointId " +
            "JOIN ApItem item ON part.partId = item.part.partId " +
            "WHERE item.deleteChange IS NULL AND item.data IN :dataRecordRefs")
    List<Integer> findAccessPointIdsByRefData(@Param("dataRecordRefs") Collection<ArrDataRecordRef> dataRecordRefs);

    @Query("SELECT s.accessPointId FROM ap_state s WHERE s.deleteChangeId IS NULL")
    List<Integer> findActiveAccessPointIds();

    @Query("SELECT s.accessPointId FROM ap_state s WHERE s.deleteChangeId IS NULL AND s.apType IN :apTypes")
    List<Integer> findActiveAccessPointIdsByApTypes(@Param("apTypes") Collection<ApType> apTypes);

    @Modifying
    @Query("UPDATE ap_access_point SET state = 'INIT' WHERE accessPointId IN :accessPointIds AND state <> 'INIT'")
    void updateToInit(@Param("accessPointIds") Collection<Integer> accessPointIds);

    @Query(value = 
            "SELECT ap.uuid FROM ap_state s" + 
            "  JOIN ap_access_point ap ON s.access_point_id = ap.access_point_id" + 
            "  WHERE s.create_change_id > :fromId OR s.delete_change_id > :fromId" + 
            " UNION " + 
            "SELECT ap.uuid FROM ap_part p" + 
            "  JOIN ap_access_point ap ON p.access_point_id = ap.access_point_id" + 
            "  WHERE p.create_change_id > :fromId OR p.delete_change_id > :fromId" + 
            " UNION " + 
            "SELECT ap.uuid FROM ap_item i" + 
            "  JOIN ap_part p ON i.part_id = p.part_id" + 
            "  JOIN ap_access_point ap ON p.access_point_id = ap.access_point_id" + 
            "  WHERE i.create_change_id > :fromId OR i.delete_change_id > :fromId" + 
            " UNION " + 
            "SELECT ap.uuid FROM ap_binding_state b" + 
            "  JOIN ap_access_point ap ON b.access_point_id = ap.access_point_id" + 
            "  WHERE b.create_change_id > :fromId OR b.delete_change_id > :fromId", nativeQuery = true)
    List<String> findAccessPointUuidChangedOrDeleted(@Param("fromId") Integer fromId);

    /**
     * Return the highest ID of ApChange used in part or item of given AccessPoint
     *  
     * @param accessPointId
     * @return
     */
    @Query(value = "SELECT MAX(u.change_id) FROM" +
            "(SELECT GREATEST(p.create_change_id, p.delete_change_id) AS change_id" +
            " FROM ap_part p WHERE p.access_point_id = ?1" +
            " UNION " + 
            " SELECT GREATEST(i.create_change_id, i.delete_change_id) AS change_id" +
            " FROM ap_item i where i.part_id IN" +
            "   (SELECT p.part_id FROM ap_part p WHERE p.access_point_id = ?1)) u", nativeQuery = true)
    int getLastChange(Integer accessPointId);
}
