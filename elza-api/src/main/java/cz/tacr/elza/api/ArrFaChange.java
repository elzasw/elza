package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Seznam provedených změn v archivních pomůckách.
 * @author vavrejn
 *
 */
public interface ArrFaChange extends Serializable {

    /**
     * 
     * @return číslo změny.
     */
    Integer getFaChangeId();

    /**
     * Nastaví číslo změny.
     * @param changeId
     */
    void setFaChangeId(Integer changeId);

    /**
     * 
     * @return datum změny.
     */
    LocalDateTime getChangeDate();

    /**
     * Nastaví datum změny.
     * @param changeDate
     */
    void setChangeDate(LocalDateTime changeDate);
}
