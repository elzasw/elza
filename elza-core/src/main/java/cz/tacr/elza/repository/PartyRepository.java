package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.projection.ParPartyInfo;


/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepository extends ElzaJpaRepository<ParParty, Integer>, PartyRepositoryCustom {

    /**
     * @param recordId id záznamu rejtříku
     * @return záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT ap FROM par_party ap JOIN ap.record r WHERE r.recordId = ?1")
    List<ParParty> findParPartyByRecordId(Integer recordId);


    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param recordIds seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT ap FROM par_party ap WHERE ap.record IN (?1)")
    List<ParParty> findParPartyByRecords(Collection<RegRecord> records);

    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param recordIds seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    @Query("SELECT ap.partyId, r.recordId FROM par_party ap JOIN ap.record r WHERE r IN (?1)")
    List<Object[]> findRecordIdAndPartyIdByRecords(Collection<RegRecord> records);


    /**
     * Najde seznam tvůrců osoby podle vytvořené osoby.
     *
     * @param party vytvořená osoba
     * @return seznam tvůrců
     */
    @Query("SELECT c.creatorParty FROM par_creator c WHERE c.party = ?1 ORDER BY c.creatorId")
    List<ParParty> findCreatorsByParty(ParParty party);

    List<ParPartyInfo> findInfoByRecordRecordIdIn(Collection<Integer> recordIds);
}
