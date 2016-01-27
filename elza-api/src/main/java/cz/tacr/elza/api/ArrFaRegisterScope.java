package cz.tacr.elza.api;

/**
 * Spojení třídy rejstříku s archivní pomůckou.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
public interface ArrFaRegisterScope<FA extends ArrFindingAid, RS extends RegScope> {

    /**
     * @return Id
     */
    Integer getFaRegisterScopeId();


    /**
     * @param faRegisterScopeId Id
     */
    void setFaRegisterScopeId(Integer faRegisterScopeId);


    /**
     * @return archivní pomůcka
     */
    FA getFindingAid();


    /**
     * @param findingAid archivní pomůcka
     */
    void setFindingAid(FA findingAid);


    /**
     * @return třída rejstříku
     */
    RS getScope();


    /**
     * @param scope Třída rejstříku
     */
    void setScope(RS scope);
}
