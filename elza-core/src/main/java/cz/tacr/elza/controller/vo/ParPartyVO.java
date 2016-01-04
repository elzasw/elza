package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * Abstraktní osoba.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyVO {

    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Rejstříkové heslo.
     */
    private RegRecordVO record;

    /**
     * Typ osoby.
     */
    private ParPartyTypeVO partyType;
    /**
     * Jednoznačné určení preferovaného jména osoby vazbou na tabulku jmen osoby.
     */
    private ParPartyNameVO preferredName;
    /**
     * Dějiny osoby.
     */
    private String history;
    /**
     * Zdroje informací.
     */
    private String sourceInformation;

    /**
     * Seznam vazeb osoby.
     */
    private List<ParRelationVO> relations;
    /**
     * Seznam působností osoby.
     */
    private List<ParPartyTimeRangeVO> timeRanges;
    /**
     * Seznam jmen osoby.
     */
    private List<ParPartyNameVO> partyNames;
    /**
     * Seznam tvůrců osoby.
     */
    private List<ParPartyVO> creators;


    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public RegRecordVO getRecord() {
        return record;
    }

    public void setRecord(final RegRecordVO record) {
        this.record = record;
    }

    public ParPartyTypeVO getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyTypeVO partyType) {
        this.partyType = partyType;
    }

    public ParPartyNameVO getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final ParPartyNameVO preferredName) {
        this.preferredName = preferredName;
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

    public List<ParRelationVO> getRelations() {
        return relations;
    }

    public void setRelations(final List<ParRelationVO> relations) {
        this.relations = relations;
    }

    public List<ParPartyTimeRangeVO> getTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(final List<ParPartyTimeRangeVO> timeRanges) {
        this.timeRanges = timeRanges;
    }

    public List<ParPartyNameVO> getPartyNames() {
        return partyNames;
    }

    public void setPartyNames(final List<ParPartyNameVO> partyNames) {
        this.partyNames = partyNames;
    }

    public List<ParPartyVO> getCreators() {
        return creators;
    }

    public void setCreators(final List<ParPartyVO> creators) {
        this.creators = creators;
    }

    public void addPartyTimeRange(final ParPartyTimeRangeVO partyTimeRange) {
        if (timeRanges == null) {
            timeRanges = new LinkedList<>();
        }
        timeRanges.add(partyTimeRange);
    }
}

