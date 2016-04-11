package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Uživatel.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
public interface UsrUser<P> extends Serializable {

    /**
     * @return identifikátor entity
     */
    Integer getUserId();

    /**
     * @param userId identifikátor entity
     */
    void setUserId(Integer userId);

    /**
     * @return vazba na osobu
     */
    P getParty();

    /**
     * @param party vazba na osobu
     */
    void setParty(P party);

    /**
     * @return uživatelské jméno
     */
    String getUsername();

    /**
     * @param username uživatelské jméno
     */
    void setUsername(String username);

    /**
     * @return uživatelské heslo
     */
    byte[] getPassword();

    /**
     * @param password uživatelské heslo
     */
    void setPassword(byte[] password);

    /**
     * @return je účet aktivní?
     */
    Boolean getActive();

    /**
     * @param active je účet aktivní?
     */
    void setActive(Boolean active);

    /**
     * @return poznámka u uživateli
     */
    String getDescription();

    /**
     * @param description poznámka u uživateli
     */
    void setDescription(String description);
}
