package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * Osoba pro operace insert.
 */
public class ParPartyVOInsert {

    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Typ osoby.
     */
    private Integer partyTypeId;

    /**
     * Dějiny osoby.
     */
    private String history;
    /**
     * Zdroje informací.
     */
    private String sourceInformation;

    /**
     * Seznam jmen osoby.
     */
    private List<ParPartyNameVOSave> partyNames;


    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(final String history) {
        this.history = history;
    }

    public String getSourceInformation() {
        return sourceInformation;
    }

    public void setSourceInformation(final String sourceInformation) {
        this.sourceInformation = sourceInformation;
    }

    public List<ParPartyNameVOSave> getPartyNames() {
        return partyNames;
    }

    public void setPartyNames(final List<ParPartyNameVOSave> partyNames) {
        this.partyNames = partyNames;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public void setPartyTypeId(Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }
}

