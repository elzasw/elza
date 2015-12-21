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
    private RegRegisterTypeVO registerType;

    /**
     * Id nadřazeného hesla.
     */
    private Integer parentRecordId;
    /**
     * Id osoby.
     */
    private Integer partyId;

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
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. Lokální heslo je přiřazené pouze konkrétnímu
     * archivnímu popisu/pomůcce.
     */
    private Boolean local;
    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     */
    private String externalId;
    /**
     * Seznam potomků.
     */
    private List<RegRecordVO> childs;

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    public RegRegisterTypeVO getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final RegRegisterTypeVO registerType) {
        this.registerType = registerType;
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

    public Boolean getLocal() {
        return local;
    }

    public void setLocal(final Boolean local) {
        this.local = local;
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

    public List<RegRecordVO> getChilds() {
        return childs;
    }

    public void setChilds(final List<RegRecordVO> childs) {
        this.childs = childs;
    }
}
