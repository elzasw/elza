package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

/**
 * Repository záznamy v rejstříku.
 */
@Repository
public interface ApAccessPointRepository
        extends ElzaJpaRepository<ApAccessPoint, Integer>, ApAccessPointRepositoryCustom {

    /**
     * Najde rejstříková hesla pro dané osoby.
     *
     * @param parties
     *            seznam osob
     * @return seznam rejstříkových hesel pro osoby
     */
    @Query("SELECT p.accessPoint FROM par_party p WHERE p IN (?1)")
    List<ApAccessPoint> findByParties(Collection<ParParty> parties);

    /**
     * Najde heslo podle UUID.
     *
     * @param uuid
     *            UUID
     * @return rejstříkové heslo
     */
    ApAccessPoint findApAccessPointByUuid(String uuid);

    /**
     * Searches not deleted APs by UUIDs.
     *
     * @return AP projection
     */
    @Query("SELECT new cz.tacr.elza.domain.projection.ApAccessPointInfo(s.accessPointId, s.accessPoint.uuid, s.scopeId, s.apTypeId)" +
            " FROM ap_state s" +
            " WHERE s.accessPoint.uuid IN (:uuids)" +
            " AND s.deleteChangeId IS NULL")
    List<ApAccessPointInfo> findActiveByUuids(@Param("uuids") Collection<String> uuids);

    @Modifying
    @Query("DELETE FROM ap_access_point ap WHERE ap.state = 'TEMP'")
    void removeTemp();

    @Query("SELECT DISTINCT ap.accessPointId FROM ap_name n JOIN n.accessPoint ap WHERE ap.state = 'INIT' OR n.state = 'INIT'")
    Set<Integer> findInitAccessPointIds();

    @Query("SELECT distinct i.itemId" +
            " FROM arr_item i" +
            " JOIN arr_data_party_ref pr ON (pr.dataId = i.dataId)" +
            " WHERE pr.party.accessPoint.accessPointId = ?1")
    List<Integer> findItemIdByAccessPointIdOverDataPartyRef(Integer accessPointId);


    @Query("SELECT distinct i.itemId" +
            " FROM arr_item i" +
            " JOIN arr_data_record_ref rr ON (rr.dataId = i.dataId)" +
            " WHERE rr.record.accessPointId = ?1")
    List<Integer> findItemIdByAccessPointIdOverDataRecordRef(Integer accessPointId);
}
