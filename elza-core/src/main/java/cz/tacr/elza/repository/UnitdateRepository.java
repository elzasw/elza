package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParUnitdate;

/**
 * Repozitory pro {@link ParUnitdate}.
 *
 * @author Tomáš Kubový
 *         [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
@Repository
public interface UnitdateRepository extends JpaRepository<ParUnitdate, Integer> {

    /**
     * Najde hodnoty validity od pro jméno osoby.
     *
     * @param parties
     *            seznam osob
     * @return hodnoty datace pro dané jména osob
     */
    @Query("SELECT u FROM par_unitdate u JOIN u.validFromPartyNames r where r.party IN (?1)")
    List<ParUnitdate> findForFromPartyNameByParties(Collection<ParParty> parties);

    /**
     * Najde hodnoty validity do pro jméno osoby.
     *
     * @param parties
     *            seznam osob
     * @return hodnoty datace pro dané jména osob
     */
    @Query("SELECT u FROM par_unitdate u JOIN u.validToPartyNames r where r.party IN (?1)")
    List<ParUnitdate> findForToPartyNameByParties(Collection<ParParty> parties);

    @Modifying
    int deleteByUnitdateIdIn(Collection<Integer> unitdateIds);
}
