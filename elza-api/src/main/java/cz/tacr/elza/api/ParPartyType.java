package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyType extends Serializable {

    /**
     * Vlastní ID.
     * @return id
     */
    Integer getPartyTypeId();

    /**
     * Vlastní ID.
     * @param partyTypeId id
     */
    void setPartyTypeId(Integer partyTypeId);

    /**
     * Kód typu osoby.
     * @return kód typu
     */
    String getCode();

    /**
     * Kód typu osoby.
     * @param code kód typu
     */
    void setCode(String code);

    /**
     * Název typu osoby.
     * @return název typu
     */
    String getName();

    /**
     * Název typu osoby.
     * @param name název typu
     */
    void setName(String name);

    /**
     * Popis.
     * @return popis
     */
    String getDescription();

    /**
     * Popis.
     * @param description popis
     */
    void setDescription(String description);

}
