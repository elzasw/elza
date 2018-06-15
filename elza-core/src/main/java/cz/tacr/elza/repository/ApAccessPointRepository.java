package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.domain.projection.ApAccessPointInfoExternal;
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
     * Najde záznamy rejstříkových hesel pro osoby se vztahem k dané osobě.
     *
     * @param party
     *            osoba
     * @return rejstříková hesla příbuzných osob
     */
    @Query("SELECT ap FROM ap_access_point ap JOIN ap.relationEntities re JOIN re.relation r WHERE r.party = ?1")
    List<ApAccessPoint> findByPartyRelations(ParParty party);

    /**
     * Najde rejstříková hesla pro dané osoby.
     *
     * @param parties
     *            seznam osob
     * @return seznam rejstříkových hesel pro osoby
     */
    @Query("SELECT p.record FROM par_party p WHERE p IN (?1)")
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
    List<ApAccessPointInfo> findByUuidIn(Collection<String> uuids);

    /**
     * Searches APs by external ids.
     * 
     * @return AP projection with external id
     */
    @Query("SELECT eid.accessPoint FROM ap_external_id eid JOIN ap_external_id_type eidType WHERE eidType.code=?1 AND eid.value IN ?2 AND eid.deleteChange IS NULL")
    List<ApAccessPointInfoExternal> findByEidTypeCodeAndEidValuesIn(String eidTypeCode, Collection<String> eidValues);
}
