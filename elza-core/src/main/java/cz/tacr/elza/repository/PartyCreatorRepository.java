package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * Repozitory pro {@link ParCreator}
 */
@Repository
public interface PartyCreatorRepository extends JpaRepository<ParCreator, Integer> {

    /**
     * Smaže tvůrce dle obou vazeb (vytvořil / koho) na p5edanou osobu.
     *
     * @param party osoba
     */
    @Query("DELETE FROM par_creator pc WHERE (pc.party = ?1 OR pc.creatorParty = ?1)")
    @Modifying
    void deleteByPartyBoth(ParParty party);
}
