package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.LinkedList;
import java.util.List;


/**
 * Abstraktní osoba.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class ParPartyVO {

    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Typ osoby.
     */
    private ParPartyTypeVO partyType;

    /**
     * Dějiny osoby.
     */
    private String history;
    /**
     * Zdroje informací.
     */
    private String sourceInformation;
    /**
     * Čas působonosti od
     */
    private ParUnitdateVO from;
    /**
     * Čas působonosti do
     */
    private ParUnitdateVO to;

    /**
     * Seznam vazeb osoby.
     */
    private List<ParRelationVO> relations;
    /**
     * Seznam jmen osoby.
     */
    private List<ParPartyNameVO> partyNames;
    /**
     * Seznam tvůrců osoby.
     */
    private List<ParPartyVO> creators;

    /**
     * Rejstříkové heslo.
     */
    private RegRecordVO record;

    /**
     * Verze záznamu.
     */
    private Integer version;


    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public ParPartyTypeVO getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyTypeVO partyType) {
        this.partyType = partyType;
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

    public ParUnitdateVO getFrom() {
        return from;
    }

    public void setFrom(ParUnitdateVO from) {
        this.from = from;
    }

    public ParUnitdateVO getTo() {
        return to;
    }

    public void setTo(ParUnitdateVO to) {
        this.to = to;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public RegRecordVO getRecord() {
        return record;
    }

    public void setRecord(final RegRecordVO record) {
        this.record = record;
    }

    public void addPartyName(final ParPartyNameVO partyName) {
        if (partyNames == null) {
            partyNames = new LinkedList<>();
        }
        partyNames.add(partyName);
    }
}

