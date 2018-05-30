package cz.tacr.elza.drools.model;

/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ změny.
 *
 * @since 27.02.2018
 */
public class StructObjItem {

    /**
     * Typ atributu
     */
    private String type;
    /**
     * Specifikace.
     */
    private String specCode;

    /**
     * Datový typ.
     */
    private String dataType;

    /**
     * Hodnota.
     */
    private Integer integerValue;

    public StructObjItem() {

    }

    public StructObjItem(final String type, final String spec) {
        this.type = type;
        this.specCode = spec;
    }

    /**
     * Copy constructor
     * @param strucutedItem kopírovaný atribut
     */
    public StructObjItem(final StructObjItem strucutedItem) {
        this.type = strucutedItem.type;
        this.specCode = strucutedItem.specCode;
        this.dataType = strucutedItem.dataType;
        this.integerValue = strucutedItem.integerValue;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
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

    public String getDataType() {
        return dataType;
    }

    public void setDataType(final String dataType) {
        this.dataType = dataType;
    }

}
