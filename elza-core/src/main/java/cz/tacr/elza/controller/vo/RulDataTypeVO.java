package cz.tacr.elza.controller.vo;

/**
 * VO datového typu.
 *
 */
public class RulDataTypeVO
        extends BaseCodeVo {
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
}
