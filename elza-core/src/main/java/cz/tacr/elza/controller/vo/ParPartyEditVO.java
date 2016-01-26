package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


/**
 * Osoba pro operace insert.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({@JsonSubTypes.Type(value = ParDynastyEditVO.class),
               @JsonSubTypes.Type(value = ParPartyGroupEditVO.class),
               @JsonSubTypes.Type(value = ParPersonEditVO.class),
               @JsonSubTypes.Type(value = ParEventEditVO.class)
})
public class ParPartyEditVO {

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
     * Působnost osoby od
     */
    private ParUnitdateEditVO from;
    /**
     * Působnost osoby do
     */
    private ParUnitdateEditVO to;

    /**
     * Seznam jmen osoby.
     */
    private List<ParPartyNameEditVO> partyNames;


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

    public List<ParPartyNameEditVO> getPartyNames() {
        return partyNames;
    }

    public void setPartyNames(final List<ParPartyNameEditVO> partyNames) {
        this.partyNames = partyNames;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }

    public ParUnitdateEditVO getFrom() {
        return from;
    }

    public void setFrom(ParUnitdateEditVO from) {
        this.from = from;
    }

    public ParUnitdateEditVO getTo() {
        return to;
    }

    public void setTo(ParUnitdateEditVO to) {
        this.to = to;
    }

}

