package cz.tacr.elza.controller.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Abstraktní osoba.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class ParPartyVO {

    /**
     * Id osoby.
     */
    private Integer id;

    /**
     * Složený název osoby
     */
    private String name;

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
    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    private List<ParPartyVO> creators;

    /**
     * Rejstříkové heslo.
     */
    private ApAccessPointVO accessPoint;

    /**
     * Charakteristika.
     */
    private String characteristics;

    /**
     * Verze záznamu.
     */
    private Integer version;


    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public ApAccessPointVO getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(final ApAccessPointVO accessPoint) {
        this.accessPoint = accessPoint;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }
}

