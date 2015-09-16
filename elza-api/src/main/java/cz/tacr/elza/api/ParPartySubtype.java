package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník podtypů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartySubtype<PT extends ParPartyType> extends Serializable {

    /**
     * Vlastní ID.
     * @return id
     */
    Integer getPartySubtypeId();

    /**
     * Vlastní ID.
     * @param partySubtypeId id
     */
    void setPartySubtypeId(Integer partySubtypeId);

    /**
     * Nadřazený typ osoby.
     * @return typ osoby
     */
    PT getPartyType();

    /**
     * Nadřazený typ osoby.
     * @param partyType typ osoby
     */
    void setPartyType(PT partyType);

    /**
     * Kód podtypu.
     * @return jednoznačný textový kód podtypu osoby
     */
    String getCode();

    /**
     * Kód podtypu.
     * @param code jednoznačný textový kód podtypu osoby
     */
    void setCode(String code);

    /**
     * Název podtypu.
     * @return název podtypu osoby
     */
    String getName();

    /**
     * Název podtypu.
     * @param name název podtypu osoby
     */
    void setName(String name);

    /**
     * Popis podtypu.
     * @return popis podtypu osoby
     */
    String getDescription();

    /**
     * Popis podtypu.
     * @param description popis podtypu osoby
     */
    void setDescription(String description);

    /**
     * Příznak, zda může být podtyp osoby původcem.
     * @return příznak, zda může být podtyp osoby původcem
     */
    Boolean getOriginator();

    /**
     * Příznak, zda může být podtyp osoby původcem.
     * @param originator příznak, zda může být podtyp osoby původcem
     */
    void setOriginator(Boolean originator);

}
