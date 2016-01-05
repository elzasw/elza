package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;


/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepository extends JpaRepository<ParParty, Integer>, PartyRepositoryCustom {

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
    @Query("SELECT ap FROM par_party ap JOIN ap.record r WHERE r.recordId IN (?1)")
    List<ParParty> findParPartyByRecordIds(Collection<Integer> recordIds);


    /**
     * Najde seznam tvůrců osoby podle vytvořené osoby.
     *
     * @param party vytvořená osoba
     * @return seznam tvůrců
     */
    @Query("SELECT c.creatorParty FROM par_creator c WHERE c.party = ?1")
    List<ParParty> findCreatorsByParty(ParParty party);

}
