package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Seznam provedených změn v archivních pomůckách.
 * @author vavrejn
 *
 */
public interface ArrChange<U extends UsrUser> extends Serializable {

    /**
     * 
     * @return číslo změny.
     */
    Integer getChangeId();

    /**
     * Nastaví číslo změny.
     * @param changeId  číslo změny.
     */
    void setChangeId(Integer changeId);

    /**
     * 
     * @return datum změny.
     */
    LocalDateTime getChangeDate();

    /**
     * Nastaví datum změny.
     * @param changeDate datum změny.
     */
    void setChangeDate(LocalDateTime changeDate);

    /**
     * @return uživatel, který provedl změnu
     */
    U getUser();

    /**
     * @param user uživatel, který provedl změnu
     */
    void setUser(U user);
}
