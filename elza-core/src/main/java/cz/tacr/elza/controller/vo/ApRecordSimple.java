package cz.tacr.elza.controller.vo;


/**
 * VO rejstříkového hesla obsahující pouze základní údaje.
 *
 * Nyní se používá v klientovi u osob při vytváření vazby
 */
public class ApRecordSimple extends AbstractApAccessPoint {
    private Integer id;
    private String record;
    private String characteristics;

    /**
     * AccessPoint type ID
     */
    private Integer typeId;

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

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer apTypeId) {
        this.typeId = apTypeId;
    }
}
