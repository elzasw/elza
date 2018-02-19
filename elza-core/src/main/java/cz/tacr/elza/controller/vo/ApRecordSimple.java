package cz.tacr.elza.controller.vo;

/**
 * VO rejstříkového hesla obsahující pouze základní údaje.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
public class ApRecordSimple extends AbstractApRecord {

    private Integer id;
    private String record;
    private String characteristics;
    private Integer apTypeId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
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

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
    }
}
