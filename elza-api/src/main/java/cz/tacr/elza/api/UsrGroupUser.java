package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Seznam uživatelů ve skupině.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
public interface UsrGroupUser<U, G> extends Serializable {

    /**
     * @return identifikátor entity
     */
    Integer getGroupUserId();

    /**
     * @param groupUserId identifikátor entity
     */
    void setGroupUserId(Integer groupUserId);

    /**
     * @return skupina
     */
    G getGroup();

    /**
     * @param group skupina
     */
    void setGroup(G group);

    /**
     * @return uživatel ve skupině
     */
    U getUser();

    /**
     * @param user uživatel ve skupině
     */
    void setUser(U user);
}
