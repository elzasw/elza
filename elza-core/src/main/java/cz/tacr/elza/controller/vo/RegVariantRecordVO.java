package cz.tacr.elza.controller.vo;

/**
 * VO Variantní rejstříkové heslo.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 07.01.2016
 */
public class RegVariantRecordVO {

    /**
     * Id.
     */
    private Integer variantRecordId;

    /**
     * Id rejstříkového hesla.
     */
    private Integer regRecordId;

    /**
     * Hodnota.
     */
    private String record;

    /**
     * Verze záznamu.
     */
    private Integer version;

    public Integer getVariantRecordId() {
        return variantRecordId;
    }

    public void setVariantRecordId(final Integer variantRecordId) {
        this.variantRecordId = variantRecordId;
    }

    public Integer getRegRecordId() {
        return regRecordId;
    }

    public void setRegRecordId(final Integer regRecordId) {
        this.regRecordId = regRecordId;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(final String record) {
        this.record = record;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
