package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;


/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends ElzaJpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

    /**
     * Najde potomky rejstříkového hesla.
     *
     * @param parentRecord rodič
     * @return seznam potomků
     */
    List<RegRecord> findByParentRecord(RegRecord parentRecord);

    /**
     * Najde potomky rejstříkových hesel.
     *
     * @param parentRecords rodiče
     *
     * @return seznam potomků
     */
    @Query("SELECT r FROM reg_record r WHERE parentRecord IN (?1)")
    List<RegRecord> findByParentRecords(List<RegRecord> parentRecords);


    @Query("SELECT r FROM reg_record r WHERE r.externalId IN (?1) "
            + "and r.externalSystem = ?2")
    List<RegRecord> findRegRecordByExternalIdsAndExternalSystem(Set<String> set, RegExternalSystem externalSystem);

    @Query("SELECT r FROM reg_record r WHERE r.externalId = ?1 "
            + "and r.externalSystem.code = ?2 and r.scope = ?3")
    RegRecord findRegRecordByExternalIdAndExternalSystemCodeAndScope(String externalId, String externalSystemCode, RegScope scope);


    /**
     * Najde záznamy rejstříkových hesel pro osoby se vztahem k dané osobě.
     *
     * @param party osoba
     * @return rejstříková hesla příbuzných osob
     */
    @Query("SELECT rec FROM reg_record rec JOIN rec.relationEntities re JOIN re.relation r WHERE r.party = ?1")
    List<RegRecord> findByPartyRelations(ParParty party);


    /**
     * Najde rejstříková hesla pro dané osoby.
     *
     * @param parties seznam osob
     * @return seznam rejstříkových hesel pro osoby
     */
    @Query("SELECT p.record FROM par_party p WHERE p IN (?1)")
    List<RegRecord> findByParties(Collection<ParParty> parties);

    /**
     * Najde hesla podle třídy rejstříku.
     *
     * @param scope třída
     * @return nalezená hesla
     */
    List<RegRecord> findByScope(RegScope scope);

    /**
     * Najde heslo podle UUID.
     *
     * @param uuid UUID
     * @return rejstříkové heslo
     */
    RegRecord findRegRecordByUuid(String uuid);

    /**
     * Najde id hesel a uuid.
     *
     * @param uuid UUID
     * @return seznam uuid, id hesla
     */
    @Query("SELECT uuid, recordId FROM reg_record WHERE uuid IN (?1)")
    List<Object[]> findUuidAndRecordIdByUuid(Collection<String> uuid);

    /**
     * Najde id hesel a externí id.
     *
     * @param externalSystemCode kód externího systému
     * @param externalIds externí id
     * @return seznam externId, id hesla
     */
    @Query("SELECT r.externalId, r.recordId FROM reg_record r WHERE r.externalSystem.code = ?1 and r.externalId IN (?2)")
    List<Object[]> findExternIdAndRecordIdBySystemCodeAndExternalIds(String externalSystemCode, Collection<String> externalIds);
}
