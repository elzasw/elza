package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.LinkedList;
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
     * Seznam jmen osoby.
     */
    private List<ParPartyNameEditVO> partyNames;

    /**
     * Seznam působností osoby.
     */
    private List<ParPartyTimeRangeEditVO> timeRanges;


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

    public List<ParPartyTimeRangeEditVO> getTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(final List<ParPartyTimeRangeEditVO> timeRanges) {
        this.timeRanges = timeRanges;
    }

    public void addPartyTimeRange(final ParPartyTimeRangeEditVO partyTimeRange) {
        if (timeRanges == null) {
            timeRanges = new LinkedList<>();
        }
        timeRanges.add(partyTimeRange);
    }

}

