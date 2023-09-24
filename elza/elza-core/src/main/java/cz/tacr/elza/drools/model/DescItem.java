package cz.tacr.elza.drools.model;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

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

    /**
     * Prvek popisu jen pro cteni
     */
    private boolean readOnly;

    private ArrDataUnitdate unitDate;

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
        this.readOnly = descItem.readOnly;
    }

    private DescItem(ArrDescItem descItem) {
        readOnly = descItem.getReadOnly() == null ? false : descItem.getReadOnly();
        undefined = descItem.isUndefined();
        descItemId = descItem.getItemId();
        type = descItem.getItemType().getCode();
        specCode = descItem.getItemSpec() == null ? null : descItem.getItemSpec().getCode();
        dataType = descItem.getItemType().getDataType().getCode();
    }

    public Integer getDescItemId() {
        return descItemId;
    }

    public String getType() {
        return type;
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

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isUndefined() {
        return undefined;
    }

    /**
     * Je atribut poděděn.
     * 
     * Příznak není vhodné využívat při vyhodnocování pravidel.
     * Hrozí omyl při použití !!!
     *
     * @return vrací příznak, zda je atribut zděděn z vyšší úrovně
     */
    public boolean isInherited() {
        return nodeId != null;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Vytvoří hodnotu atributu.
     *
     * @param descItem atribut
     * @return vo hodnota atributu
     */
    static public DescItem valueOf(final ArrDescItem descItem) {
        DescItem item = new DescItem(descItem);
        return item;
    }

    public void setUnitDate(final ArrDataUnitdate unitDate) {
        this.unitDate = unitDate;
    }

    public Long getNormalizedFrom() {
        if (this.unitDate == null) {
            throw new BusinessException("Item is not unitDate, dataType: " + this.dataType
                    + ", itemType: " + this.type,
                    BaseCode.INVALID_STATE);
        }
        return unitDate.getNormalizedFrom();
    }

    public Long getNormalizedTo() {
        if (this.unitDate == null) {
            throw new BusinessException("Item is not unitDate, dataType: " + this.dataType
                    + ", itemType: " + this.type,
                    BaseCode.INVALID_STATE);
        }
        return unitDate.getNormalizedTo();
    }

}
