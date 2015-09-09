package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník podtypů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartySubtype<PT extends ParPartyType> extends Serializable {

    Integer getPartySubtypeId();

    void setPartySubtypeId(Integer partySubtypeId);

    PT getPartyType();

    void setPartyType(PT partyType);

    /**
     * @return jednoznačný textový kód podtypu osoby.
     */
    String getCode();

    /**
     * @param code jednoznačný textový kód podtypu osoby.
     */
    void setCode(String code);

    /**
     * @return název podtypu osoby.
     */
    String getName();

    /**
     * @param name název podtypu osoby.
     */
    void setName(String name);

    /**
     * @return popis podtypu osoby.
     */
    String getDescription();

    /**
     * @param description popis podtypu osoby.
     */
    void setDescription(String description);

    /**
     * @return příznak, zda může být podtyp osoby původcem.
     */
    Boolean getOriginator();

    /**
     * @param originator příznak, zda může být podtyp osoby původcem.
     */
    void setOriginator(Boolean originator);
}
