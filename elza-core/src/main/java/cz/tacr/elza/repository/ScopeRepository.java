package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RegScope;


/**
 * Repository pro {@link RegScope}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Repository
public interface ScopeRepository extends ElzaJpaRepository<RegScope, Integer> {

    /**
     * Najde třídy podle kódů.
     *
     * @param codes seznam kódů
     * @return seznam tříd
     */
    @Query("SELECT s FROM reg_scope s WHERE s.code IN (?1)")
    List<RegScope> findByCodes(Collection<String> codes);


    /**
     * Najde id tříd pro FA.
     *
     * @param fund archivní pomůcka
     * @return id tříd dané fa
     */
    @Query("SELECT s.scopeId FROM arr_fund_register_scope fs JOIN fs.scope s WHERE fs.fund = ?1")
    Set<Integer> findIdsByFund(final ArrFund fund);


    /**
     * Najde všechny třídy seřazené podle kodu.
     *
     * @return seznam tříd
     */
    @Query("SELECT s FROM reg_scope s ORDER BY s.code ASC")
    List<RegScope> findAllOrderByCode();

    /**
     * Najde třídu podle kódu.
     *
     * @param code kód
     * @return třída
     */
    RegScope findByCode(String string);
}
