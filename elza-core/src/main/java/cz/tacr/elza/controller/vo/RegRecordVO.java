package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * VO rejstříkového záznamu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class RegRecordVO {

    /**
     * Id hesla.
     */
    private Integer recordId;
    /**
     * Typ rejstříku.
     */
    private Integer registerTypeId;

    /**
     * Id nadřazeného hesla.
     */
    private Integer parentRecordId;
    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Id třídy rejstříku.
     */
    private Integer scopeId;

    /**
     * Externí zdroj hesel.
     */
    private RegExternalSourceVO externalSource;

    /**
     * Rejstříkové heslo.
     */
    private String record;

    /**
     * Podrobná charakteristika rejstříkového hesla.
     */
    private String characteristics;
    /**
     * Poznámka k heslu v rejstříku.
     */
    private String note;
    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     */
    private String externalId;

    /**
     * Verze záznamu.
     */
    private Integer version;

    /**
     * Seznam potomků.
     */
    private List<RegRecordVO> childs;

    /**
     * Seznam variantních rejstříkových hesel.
     */
    private List<RegVariantRecordVO> variantRecords;

    /** Cesta od toho rejstříku až ke kořeni. První záznam je kořenový rejstřík, poslední záznam je přímý nadřízený. */
    private List<RegRecordParentVO> parents;

    /** Příznak zda má rejstřík potomky. Pokud je null, nebyl příznak nastaven. */
    private Boolean hasChildren;

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getRegisterTypeId() {
        return registerTypeId;
    }

    public void setRegisterTypeId(final Integer registerTypeId) {
        this.registerTypeId = registerTypeId;
    }

    public Integer getParentRecordId() {
        return parentRecordId;
    }

    public void setParentRecordId(final Integer parentRecordId) {
        this.parentRecordId = parentRecordId;
    }

    public RegExternalSourceVO getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(final RegExternalSourceVO externalSource) {
        this.externalSource = externalSource;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(final String record) {
        this.record = record;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public List<RegRecordVO> getChilds() {
        return childs;
    }

    public void setChilds(final List<RegRecordVO> childs) {
        this.childs = childs;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public List<RegVariantRecordVO> getVariantRecords() {
        return variantRecords;
    }

    public void setVariantRecords(final List<RegVariantRecordVO> variantRecords) {
        this.variantRecords = variantRecords;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public List<RegRecordParentVO> getParents() {
        return parents;
    }

    public void setParents(List<RegRecordParentVO> parents) {
        this.parents = parents;
    }
}
