package cz.tacr.elza.controller.vo;

/**
 * VO Variantní rejstříkové heslo.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 07.01.2016
 */
public class ApVariantRecordVO {

    /**
     * Id.
     */
    private Integer id;

    /**
     * Id rejstříkového hesla.
     */
    private Integer apRecordId;

    /**
     * Hodnota.
     */
    private String record;

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

    public Integer getApRecordId() {
        return apRecordId;
    }

    public void setApRecordId(final Integer apRecordId) {
        this.apRecordId = apRecordId;
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
