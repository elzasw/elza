package cz.tacr.elza.api;

/**
 * Třída rejstříku.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
public interface RegScope {

    Integer getScopeId();


    void setScopeId(Integer scopeId);


    /**
     * @return Kód třídy rejstříku.
     */
    String getCode();


    /**
     * @param code Kód třídy rejstříku.
     */
    void setCode(String code);


    /**
     * @return Název třídy rejstříku.
     */
    String getName();


    /**
     * @param name Název třídy rejstříku.
     */
    void setName(String name);
}
