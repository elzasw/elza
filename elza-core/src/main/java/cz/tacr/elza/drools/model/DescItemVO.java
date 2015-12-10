package cz.tacr.elza.drools.model;

/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ změny.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class DescItemVO {

    /**
     * Id atributu.
     */
    private Integer descItemId;

    /**
     * Typ atributu
     */
    private String type;
    /**
     * Specifikace.
     */
    private String specCode;

    /**
     * Hodnota.
     */
    private Integer integerValue;

    /**
     * Obal.
     */
    private VOPacket packet;


    /**
     * Typ změny atributu.
     */
    private DescItemChange change;

    public DescItemVO() {

    }

    public DescItemVO(final String type, final String spec) {
        this.type = type;
        this.specCode = spec;
    }

    public Integer getDescItemId() {
        return descItemId;
    }

    public void setDescItemId(final Integer descItemId) {
        this.descItemId = descItemId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public DescItemChange getChange() {
        return change;
    }

    public void setChange(final DescItemChange change) {
        this.change = change;
    }

    public String getSpecCode() {
        return specCode;
    }

    public void setSpecCode(final String specCode) {
        this.specCode = specCode;
    }

    public Integer getInteger() {
        return integerValue;
    }

    public void setInteger(final Integer value) {
        this.integerValue = value;
    }

    public VOPacket getPacket() {
        return packet;
    }

    public void setPacket(final VOPacket packet) {
        this.packet = packet;
    }
}
