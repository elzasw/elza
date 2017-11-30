package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParParty;


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


    /**
     * Najde tvůrce podle osoby.
     *
     * @param party osoba, kterou tvůrce vytvořil
     * @return seznam tvůrců
     */
    List<ParCreator> findByParty(ParParty party);


    /**
     * Najde seznam osob vytvořených předanou osobou.
     *
     * @param party autor
     * @return seznam osob vytvořených předanou osobou
     */
    @Query("SELECT c FROM par_creator c JOIN FETCH c.party WHERE c.creatorParty = ?1")
    List<ParCreator> findByCreatorParty(ParParty party);
}
