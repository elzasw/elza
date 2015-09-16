package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Abstraktní osoba.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParParty<RR extends RegRecord, PS extends ParPartySubtype> extends Versionable, Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getPartyId();

    /**
     * Primární ID.
     * @param partyId   id objektu
     */
    void setPartyId(Integer partyId);

    /**
     * Rejstříkové heslo.
     * @return  objekt navázaného rejstříkového hesla
     */
    RR getRecord();

    /**
     * Rejstříkové heslo.
     * @param record    objekt navázaného rejstříkového hesla
     */
    void setRecord(RR record);

    /**
     * Podtyp osoby.
     * @return  objekt navázaného podtypu osoby
     */
    PS getPartySubtype();

    /**
     * Podtyp osoby.
     * @param partySubtype  objekt navázaného podtypu osoby
     */
    void setPartySubtype(PS partySubtype);
}
