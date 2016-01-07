package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


/**
 * Osoba pro operace insert.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(@JsonSubTypes.Type(value = ParDynastyVOInsert.class, name = "ParDynastyVOInsert"))
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

