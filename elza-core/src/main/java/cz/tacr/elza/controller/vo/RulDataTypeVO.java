package cz.tacr.elza.controller.vo;

/**
 * VO datového typu.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class RulDataTypeVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * kód
     */
    private String code;

    /**
     * název
     */
    private String name;

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

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

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
