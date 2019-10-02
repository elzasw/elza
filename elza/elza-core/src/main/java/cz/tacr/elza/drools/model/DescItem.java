package cz.tacr.elza.drools.model;

/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ změny.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class DescItem {

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
     * Datový typ.
     */
    private String dataType;

    /**
     * Hodnota.
     */
    private Integer integerValue;

    /**
     * Strukt. hodnota.
     */
    private Structured structured;

    /**
     * Typ změny atributu.
     */
    private DescItemChange change;

    /**
     * Identifikátor nodu, kdy byl atribut přidán.
     */
    private Integer nodeId;

    /**
     * Nedefinovaná hodnota atributu?
     */
    private boolean undefined;

    public DescItem() {

    }

    public DescItem(final String type, final String spec) {
        this.type = type;
        this.specCode = spec;
    }

    /**
     * Copy constructor
     * @param descItem kopírovaný atribut
     */
    public DescItem(final DescItem descItem) {
        this.descItemId = descItem.descItemId;
        this.type = descItem.type;
        this.specCode = descItem.specCode;
        this.dataType = descItem.dataType;
        this.integerValue = descItem.integerValue;
        this.structured = descItem.structured;
        this.change = descItem.change;
        this.nodeId = descItem.nodeId;
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

    public Structured getStructured() {
        return structured;
    }

    public void setStructured(final Structured structured) {
        this.structured = structured;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(final String dataType) {
        this.dataType = dataType;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isUndefined() {
        return undefined;
    }

    public void setUndefined(final boolean undefined) {
        this.undefined = undefined;
    }

    /**
     * Je atribut označen jako efektivní?
     *
     * @return je efektivní?
     */
    public boolean isInherited() {
        return nodeId != null;
    }
}
