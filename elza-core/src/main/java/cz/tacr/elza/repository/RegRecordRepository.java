package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;


/**
 * Repository záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegRecordRepository extends JpaRepository<RegRecord, Integer>, RegRecordRepositoryCustom {

    /**
     * Najde potomky rejstříkového hesla.
     *
     * @param parentRecord rodič
     * @return seznam potomků
     */
    List<RegRecord> findByParentRecord(RegRecord parentRecord);


    @Query("SELECT r FROM reg_record r WHERE r.externalId = ?1 "
            + "and r.externalSource.code = ?2")
    RegRecord findRegRecordByExternalIdAndExternalSourceCode(String externalId, String externalSourceCode);


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
}
