package cz.tacr.elza.controller.vo;

/**
 * VO Souřadnice
 *
 * @author Petr Compel
 * @since 21.4.2016
 */
public class RegCoordinatesVO {

    /**
     * Id.
     */
    private Integer coordinatesId;

    /**
     * Id rejstříkového hesla.
     */
    private Integer regRecordId;

    /**
     * Hodnota.
     */
    private String value;

    /**
     * Popisek.
     */
    private String description;

    public Integer getCoordinatesId() {
        return coordinatesId;
    }

    public void setCoordinatesId(Integer coordinatesId) {
        this.coordinatesId = coordinatesId;
    }

    public Integer getRegRecordId() {
        return regRecordId;
    }

    public void setRegRecordId(Integer regRecordId) {
        this.regRecordId = regRecordId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
