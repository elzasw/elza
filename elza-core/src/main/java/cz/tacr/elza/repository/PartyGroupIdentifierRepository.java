package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repozitory pro {@link ParPartyGroupIdentifier}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface PartyGroupIdentifierRepository extends JpaRepository<ParPartyGroupIdentifier, Integer> {

    /**
     * Najde seznam identifikací.
     *
     * @param party group party
     * @return seznam identifikací group party
     */
    List<ParPartyGroupIdentifier> findByPartyGroup(ParPartyGroup party);

    /**
     * Vrátí identifikátor korporace/skupiny osoby.
     * @param partyGroup skupina osob
     * @return  identifikace korporace/skupiny
     */
    @Query("SELECT pi FROM par_party_group_identifier pi WHERE pi.partyGroup = ?1")
    List<ParPartyGroupIdentifier> findByParty(ParPartyGroup partyGroup);
    
}
