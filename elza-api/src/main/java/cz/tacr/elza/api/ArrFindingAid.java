package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFaVersion}.

 * @author vavrejn
 *
 */
public interface ArrFindingAid extends Versionable, Serializable {

    Integer getFindingAidId();

    void setFindingAidId(Integer findingAidId);

    /**
     * 
     * @return Jméno AP.
     */
    String getName();

    /**
     * Nastaví jméno AP.
     * @param name
     */
    void setName(String name);

    /**
     * 
     * @return datum založení.
     */
    LocalDateTime getCreateDate();

    /**
     * Nastaví datum založení.
     * @param createDate
     */
    void setCreateDate(LocalDateTime createDate);
}
