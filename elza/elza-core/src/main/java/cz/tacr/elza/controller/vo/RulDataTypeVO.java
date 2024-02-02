package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulDataType;

/**
 * VO datového typu.
 *
 */
public class RulDataTypeVO extends BaseCodeVo {
    /**
     * popis
     */
    private String description;

    /**
     * lze použít regulární výraz?
     */
    private Boolean regexpUse;

    /**
     * lze použít limitaci délky textu?
     */
    private Boolean textLengthLimitUse;

    /**
     * tabulka pro uložení
     */
    private String storageTable;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getRegexpUse() {
        return regexpUse;
    }

    public void setRegexpUse(final Boolean regexpUse) {
        this.regexpUse = regexpUse;
    }

    public Boolean getTextLengthLimitUse() {
        return textLengthLimitUse;
    }

    public void setTextLengthLimitUse(final Boolean textLengthLimitUse) {
        this.textLengthLimitUse = textLengthLimitUse;
    }

    public String getStorageTable() {
        return storageTable;
    }

    public void setStorageTable(final String storageTable) {
        this.storageTable = storageTable;
    }

    public static RulDataTypeVO newInstance(final RulDataType dataType) {
    	RulDataTypeVO result = new RulDataTypeVO();
    	result.setId(dataType.getDataTypeId());
    	result.setName(dataType.getName());
    	result.setCode(dataType.getCode());
    	result.setDescription(dataType.getDescription());
    	result.setRegexpUse(dataType.getRegexpUse());
    	result.setStorageTable(dataType.getStorageTable());
    	result.setTextLengthLimitUse(dataType.getTextLengthLimitUse());
    	return result;
    }
}
