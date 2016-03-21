package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFundVersion}.

 * @author vavrejn
 *
 */
public interface ArrFund<I extends ParInstitution> extends Versionable, Serializable {

    Integer getFundId();

    void setFundId(Integer fundId);

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

    /**
     * @return interní kód
     */
    String getInternalCode();

    /**
     * @param internalCode interní kód
     */
    void setInternalCode(String internalCode);

    /**
     *
     * @return instituce
     */
    I getInstitution();

    /**
     * @param institution instituce
     */
    void setInstitution(I institution);
}
