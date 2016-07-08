package cz.tacr.elza.api;

import cz.tacr.elza.api.interfaces.IRegScope;

import java.io.Serializable;

/**
 * Abstraktní osoba.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParParty<RR extends RegRecord, PPT extends ParPartyType, PPN extends ParPartyName> extends Versionable, Serializable, IRegScope {

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
     * Jednoznačné určení preferovaného jména osoby vazbou na tabulku jmen osoby.
     * @param preferredName
     */
    void setPreferredName(PPN preferredName);

    /**
     * Jednoznačné určení preferovaného jména osoby vazbou na tabulku jmen osoby.
     * @return Jednoznačné určení preferovaného jména osoby vazbou na tabulku jmen osoby.
     */
    PPN getPreferredName();

    /**
     * Typ osoby.
     * @param partyType
     */
    void setPartyType(PPT partyType);

    /**
     * Typ osoby.
     * @return typ osoby.
     */
    PPT getPartyType();

    /**
     * Dějiny osoby.
     * @return dějiny osoby
     */
    String getHistory();

    /**
     * Dějiny osoby.
     * @param history dějiny osoby
     */
    void setHistory(String history);

    /**
     * Zdroje informací.
     * @return  zdroje informací
     */
    String getSourceInformation();

    /**
     * Zdroje informací.
     * @param sourceInformation zdroje informací
     */
    void setSourceInformation(String sourceInformation);
}
