package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface ApAccessPointRepository
        extends ElzaJpaRepository<ApAccessPoint, Integer>, ApAccessPointRepositoryCustom {
    @Query("SELECT ap FROM ap_access_point ap " + "JOIN ap_external_id eid on ap.accessPointId = eid.accessPointId "
            + "join eid.createChange chng " + "join chng.externalSystem esys "
            + "WHERE eid.value IN (?1) and esys = ?2 and eid.deleteChange is null")
    List<ApAccessPoint> findApAccessPointByExternalIdsAndExternalSystem(Set<String> set,
                                                                        ApExternalSystem externalSystem);

    @Query("select ap from ap_access_point ap " + "join ap_external_id eid on ap.accessPointId = eid.accessPointId "
            + "join eid.createChange chng " + "join chng.externalSystem esys "
            + "WHERE eid.value = ?1 and eid.deleteChange is null and esys.code = ?2 and ap.scope = ?3")
    ApAccessPoint findApAccessPointByExternalIdAndExternalSystemCodeAndScope(String externalId,
                                                                             String externalSystemCode, ApScope scope);

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
     * Searches APs by UUIDs.
     *
     * @return AP projection
     */
    List<ApAccessPointInfo> findInfoByUuidIn(Collection<String> uuids);
}
