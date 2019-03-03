package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

/**
 * Repository záznamy v rejstříku.
 */
@Repository
public interface ApAccessPointRepository
        extends ElzaJpaRepository<ApAccessPoint, Integer>, ApAccessPointRepositoryCustom {

    @Query("select ap from ap_access_point ap "
            + "join ap_external_id eid on ap.accessPointId = eid.accessPointId and eid.value=?1 and eid.externalIdTypeId=?2 and eid.deleteChangeId is null "
            + "WHERE ap.scope = ?3")
    ApAccessPoint findApAccessPointByExternalIdAndExternalSystemCodeAndScope(String eidValue, Integer eidTypeId, ApScope scope);

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
     * Najde hesla podle třídy rejstříku.
     *
     * @param scope
     *            třída
     * @return nalezená hesla
     */
    List<ApAccessPoint> findByScope(ApScope scope);

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
    List<ApAccessPointInfo> findInfoByUuidInAndDeleteChangeIdIsNull(Collection<String> uuids);

    @Modifying
    @Query("UPDATE ap_access_point ap SET ap.apType = :value WHERE ap.apType = :key")
    void updateApTypeByApType(@Param("key") ApType key, @Param("value") ApType value);

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
