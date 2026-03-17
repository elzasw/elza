package cz.tacr.elza.drools.model;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ změny.
 *
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
    private RulItemType itemType;
    /**
     * Specifikace.
     */
    private RulItemSpec itemSpec;

    /**
     * Datový typ.
     */
    private String dataType;

    /**
     * Hodnota.
     */
    private Integer integerValue;

    /**
     * String value
     */
    private String stringValue;

    /**
     * Strukt. hodnota.
     */
    private Structured structured;
    
    /**
     * Access point
     * 
     * Contains only basic information
     */
    private Ap ap;

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

    /**
     * Object item id
     * 
     * It might be used to pair with inhibited items
     */
	private Integer itemObjectId;

    public DescItem(final RulItemType itemType, final RulItemSpec specType) {
        this.itemType = itemType;
        this.itemSpec = specType;
    }

    /**
     * Copy constructor
     * @param descItem kopírovaný atribut
     */
    public DescItem(final DescItem descItem) {
        this.descItemId = descItem.descItemId;
        this.itemType = descItem.itemType;
        this.itemSpec = descItem.itemSpec;
        this.dataType = descItem.dataType;
        this.integerValue = descItem.integerValue;
        this.structured = descItem.structured;
        this.ap = descItem.ap;
        this.change = descItem.change;
        this.nodeId = descItem.nodeId;
        this.readOnly = descItem.readOnly;
    }

    private DescItem(ArrDescItem descItem) {
    	this.readOnly = descItem.getReadOnly() == null ? false : descItem.getReadOnly();
        this.undefined = descItem.isUndefined();
        this.descItemId = descItem.getItemId();
        this.itemType = descItem.getItemType();
        this.itemSpec = descItem.getItemSpec();
        this.itemObjectId = descItem.getDescItemObjectId();
        this.dataType = DataType.fromId(descItem.getItemType().getDataTypeId()).getCode();
    }

    public Integer getDescItemId() {
        return descItemId;
    }
    
    public Integer getItemObjectId() {
    	return itemObjectId;
    }

    public String getType() {
        return itemType.getCode();
    }
    
    public String getTypeName() {
    	return itemType.getName();
    }

    public String getTypeShortcut() {
    	return itemType.getShortcut();
    }

    public String getSpecName() {
    	return itemSpec == null ? null : itemSpec.getName();
    }

    public String getSpecShortcut() {
    	return itemSpec == null ? null : itemSpec.getShortcut();
    }

    public DescItemChange getChange() {
        return change;
    }

    public void setChange(final DescItemChange change) {
        this.change = change;
    }

    public String getSpecCode() {
    	if(itemSpec == null) {
	    	return null;
    	}
        return itemSpec.getCode();
    }

    public Integer getInteger() {
        return integerValue;
    }

    public void setInteger(final Integer value) {
        this.integerValue = value;
    }

    public String getString() {
        return stringValue;
    }

    public void setString(String value) {
        stringValue = value;
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
    
	public Ap getAp() {
		return ap;
	}
	
	public void setAp(Ap ap) {
		this.ap = ap;
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
                    + ", itemType: " + this.itemType.getCode(),
                    BaseCode.INVALID_STATE);
        }
        return unitDate.getNormalizedFrom();
    }

    public Long getNormalizedTo() {
        if (this.unitDate == null) {
            throw new BusinessException("Item is not unitDate, dataType: " + this.dataType
                    + ", itemType: " + this.itemType.getCode(),
                    BaseCode.INVALID_STATE);
        }
        return unitDate.getNormalizedTo();
    }

}
