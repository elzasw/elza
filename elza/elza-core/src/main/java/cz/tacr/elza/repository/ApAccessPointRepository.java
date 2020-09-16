package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

/**
 * Repository záznamy v rejstříku.
 */
@Repository
public interface ApAccessPointRepository
        extends ElzaJpaRepository<ApAccessPoint, Integer>, ApAccessPointRepositoryCustom {

    /**
     * Najde heslo podle UUID.
     *
     * @param uuid
     *            UUID
     * @return rejstříkové heslo
     */
    ApAccessPoint findApAccessPointByUuid(String uuid);

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
            " AND s.deleteChangeId IS NULL")
    List<ApAccessPointInfo> findActiveInfoByUuids(@Param("uuids") Collection<String> uuids);

    @Modifying
    @Query("DELETE FROM ap_access_point ap WHERE ap.state = 'TEMP'")
    void removeTemp();

    @Query("SELECT DISTINCT ap.accessPointId FROM ap_access_point ap WHERE ap.state = 'INIT'")
    Set<Integer> findInitAccessPointIds();

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

}
