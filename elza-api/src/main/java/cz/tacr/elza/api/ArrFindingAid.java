package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFindingAidVersion}.

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
     * @param name jméno AP.
     */
    void setName(String name);

    /**
     * 
     * @return datum založení.
     */
    LocalDateTime getCreateDate();

    /**
     * Nastaví datum založení.
     * @param createDate datum založení.
     */
    void setCreateDate(LocalDateTime createDate);
}
