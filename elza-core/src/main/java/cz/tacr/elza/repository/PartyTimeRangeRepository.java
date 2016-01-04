package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyTimeRange;


/**
 * Repozitory pro {@link ParPartyTimeRange}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface PartyTimeRangeRepository extends JpaRepository<ParPartyTimeRange, Integer> {

    /**
     * Najde objekty podle osoby.
     *
     * @param party osoba
     * @return seznam působností osoby
     */
    List<ParPartyTimeRange> findByParty(ParParty party);


    /**
     * Najde podle seznamu osob.
     *
     * @param parties seznam osob
     * @return seznam působností osob
     */
    @Query("SELECT p FROM par_party_time_range p WHERE p.party IN (?1)")
    List<ParPartyTimeRange> findByParties(Collection<ParParty> parties);
}
