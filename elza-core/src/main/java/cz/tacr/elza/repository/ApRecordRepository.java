package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApScope;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.projection.ApRecordInfo;
import cz.tacr.elza.domain.projection.ApRecordInfoExternal;


/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface ApRecordRepository extends ElzaJpaRepository<ApRecord, Integer>, ApRecordRepositoryCustom {
    @Query("SELECT r FROM ap_record r WHERE r.externalId IN (?1) "
            + "and r.externalSystem = ?2")
    List<ApRecord> findApRecordByExternalIdsAndExternalSystem(Set<String> set, ApExternalSystem externalSystem);

    @Query("SELECT r FROM ap_record r WHERE r.externalId = ?1 "
            + "and r.externalSystem.code = ?2 and r.scope = ?3")
    ApRecord findApRecordByExternalIdAndExternalSystemCodeAndScope(String externalId, String externalSystemCode, ApScope scope);


    /**
     * Najde záznamy rejstříkových hesel pro osoby se vztahem k dané osobě.
     *
     * @param party osoba
     * @return rejstříková hesla příbuzných osob
     */
    @Query("SELECT rec FROM ap_record rec JOIN rec.relationEntities re JOIN re.relation r WHERE r.party = ?1")
    List<ApRecord> findByPartyRelations(ParParty party);


    /**
     * Najde rejstříková hesla pro dané osoby.
     *
     * @param parties seznam osob
     * @return seznam rejstříkových hesel pro osoby
     */
    @Query("SELECT p.record FROM par_party p WHERE p IN (?1)")
    List<ApRecord> findByParties(Collection<ParParty> parties);

    /**
     * Najde hesla podle třídy rejstříku.
     *
     * @param scope třída
     * @return nalezená hesla
     */
    List<ApRecord> findByScope(ApScope scope);

    /**
     * Najde heslo podle UUID.
     *
     * @param uuid UUID
     * @return rejstříkové heslo
     */
    ApRecord findApRecordByUuid(String uuid);

    /**
     * Searches records by uuid collection.
     *
     * @return Records projections.
     */
    List<ApRecordInfo> findByUuidIn(Collection<String> uuids);

    /**
     * Searches records by external system and collection of external ids.
     *
     * @return Records projections.
     */
    List<ApRecordInfoExternal> findByExternalSystemCodeAndExternalIdIn(String externalSystemCode, Collection<String> externalIds);
}
