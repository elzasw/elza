package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;

import javax.persistence.LockModeType;

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
    @Query("DELETE FROM ap_access_point ap WHERE ap.state = 'TEMP'")
    void removeTemp();

    @Query("SELECT DISTINCT ap.accessPointId FROM ap_name n JOIN n.accessPoint ap WHERE ap.state = 'INIT' OR n.state = 'INIT'")
    Set<Integer> findInitAccessPointIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ap FROM ap_access_point ap where ap.accessPointId = :accessPointId")
    ApAccessPoint findOneWithLock(@Param("accessPointId") Integer accessPointId);
}
