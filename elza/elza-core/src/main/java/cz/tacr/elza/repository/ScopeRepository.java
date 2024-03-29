package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ApScope;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;


/**
 * Repository pro {@link ApScope}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Repository
public interface ScopeRepository extends ElzaJpaRepository<ApScope, Integer>, ScopeRepositoryCustom {

    /**
     * Najde seznam tříd podle seznamu kódů.
     *
     * @param codes seznam kódů
     * @return seznam tříd
     */
    @Query("SELECT s FROM ap_scope s WHERE s.code IN (?1)")
    List<ApScope> findByCodes(Collection<String> codes);

    /**
     * Najde seznam id tříd pro FA.
     *
     * @param fundId archivní pomůcka
     * @return id tříd dané fa
     */
    @Query("SELECT s.scopeId FROM arr_fund_register_scope fs JOIN fs.scope s WHERE fs.fund.id = ?1")
    Set<Integer> findIdsByFundId(final Integer fundId);

    /**
     * Najde seznam id podle FA spolu s připojenými.
     * 
     * @param fundId
     * @return seznam id
     */
    @Query(value = 
            "WITH fund_scopes(scope_id) AS"
            + "  (SELECT scope_id AS scope FROM arr_fund_register_scope WHERE fund_id = ?1)"
            + "SELECT scope_id from fund_scopes "
            + "UNION "
            + "SELECT connected_scope_id AS scope FROM ap_scope_relation WHERE scope_id IN"
            + "  (SELECT scope_id FROM fund_scopes)", nativeQuery = true)    
    Set<Integer> findAllConnectedByFundId(final Integer fundId);

    /**
     * Najde id tříd pro FA.
     *
     * @param fund archivní pomůcka
     * @return id tříd dané fa
     */
    @Query("SELECT s.scopeId FROM arr_fund_register_scope fs JOIN fs.scope s WHERE fs.fund = ?1")
    Set<Integer> findIdsByFund(final ArrFund fund);

    /**
     * Najde seznam tříd pro FA.
     *
     * @param fund archivní pomůcka
     * @return id tříd dané fa
     */
    @Query("SELECT s FROM arr_fund_register_scope fs JOIN fs.scope s WHERE fs.fund = ?1")
    Set<ApScope> findByFund(final ArrFund fund);

    /**
     * Najde kódy tříd pro FA.
     *
     * @param fund archivní pomůcka
     * @return id tříd dané fa
     */
    @Query("SELECT s.code FROM arr_fund_register_scope fs JOIN fs.scope s WHERE fs.fund = ?1")
    Set<String> findCodesByFund(final ArrFund fund);

    /**
     * Najde všechny třídy seřazené podle kodu.
     *
     * @return seznam tříd
     */
    @Query("SELECT s FROM ap_scope s ORDER BY s.code ASC")
    List<ApScope> findAllOrderByCode();

    /**
     * Najde třídu podle kódu.
     *
     * @param code kód
     * @return třída
     */
    ApScope findByCode(String code);

    /**
     * Najde id všech tříd.
     *
     * @return id tříd
     */
    @Query("SELECT s.scopeId FROM ap_scope s")
    Set<Integer> findAllIds();

    /**
     * Najde třídy navázané na danou třídu.
     *
     * @param scope třída
     * @return navázané třídy
     */
    @Query("SELECT r.connectedScope FROM ap_scope_relation r WHERE r.scope = ?1")
    List<ApScope> findConnectedByScope(final ApScope scope);

    List<ApScope> findScopeByRuleSetIdIsNull();
}
