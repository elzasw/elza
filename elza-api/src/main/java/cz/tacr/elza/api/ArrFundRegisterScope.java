package cz.tacr.elza.api;

/**
 * Spojení třídy rejstříku s archivní pomůckou.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
public interface ArrFundRegisterScope<FA extends ArrFund, RS extends RegScope> {

    /**
     * @return Id
     */
    Integer getFundRegisterScopeId();


    /**
     * @param fundRegisterScopeId Id
     */
    void setFundRegisterScopeId(Integer fundRegisterScopeId);


    /**
     * @return archivní pomůcka
     */
    FA getFund();


    /**
     * @param fund archivní pomůcka
     */
    void setFund(FA fund);


    /**
     * @return třída rejstříku
     */
    RS getScope();


    /**
     * @param scope Třída rejstříku
     */
    void setScope(RS scope);
}
