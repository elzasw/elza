package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
    @Query("SELECT p FROM par_party_name p WHERE p.party IN (?1)")
    List<ParPartyName> findByParties(Collection<ParParty> parties);
}
