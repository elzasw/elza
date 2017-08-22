package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;

/**
 * Repository pro abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */

public interface PartyNameRepository extends JpaRepository<ParPartyName, Integer>, PartyNameCustomRepository {

    List<ParPartyName> findByParty(ParParty party);

    /**
     * Najde seznam jmen pro dané osoby.
     *
     * @param parties seznam osob
     * @return seznam jmen osob
     */
    List<ParPartyName> findByPartyIn(Collection<ParParty> parties);

    /**
     * Native query which sets preferred name references to null. Query is used for removing
     * constraint before deleting party names.
     */
    @Modifying
    @Query(value = "UPDATE par_party SET preferred_name_id=null WHERE preferred_name_id IN (?1)", nativeQuery = true)
    int deletePreferredNameReferencesByPartyNameIdIn(Collection<Integer> partyNameIds);

    /**
     * Native query which deletes references from party name to complement. Query is used for
     * removing constraint before deleting party names.
     */
    @Modifying
    @Query(value = "DELETE FROM par_party_name_complement WHERE party_name_id IN (?1)", nativeQuery = true)
    int deleteComplementReferencesByPartyNameIdIn(Collection<Integer> partyNameIds);
}
