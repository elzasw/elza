package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repozitory pro {@link ParPartyNameComplement}.
 *
 * @author Tomáš Kubový
 *         [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface PartyNameComplementRepository extends JpaRepository<ParPartyNameComplement, Integer> {

    /**
     * Najde seznam doplňků jména podle jména.
     *
     * @param partyName
     *            jméno
     * @return seznam doplňků jména
     */
    List<ParPartyNameComplement> findByPartyName(ParPartyName partyName);

    /*
     * Smaže doplňky jmen dle daného jména.
     *
     * @param partyName jméno
     */
    @Query("DELETE FROM par_party_name_complement pnc WHERE pnc.partyName = ?1")
    @Modifying
    void deleteByPartyName(ParPartyName partyName);

    @Modifying
    void deleteByPartyNameIdIn(Collection<Integer> partyNameIds);

    @Query("SELECT pnc FROM par_party_name_complement pnc JOIN FETCH pnc.complementType ct WHERE pnc.partyName.partyNameId IN :partyNameIds")
    List<ParPartyNameComplement> findByPartyNameIds(@Param("partyNameIds") Iterable<Integer> partyNameIds);
}
