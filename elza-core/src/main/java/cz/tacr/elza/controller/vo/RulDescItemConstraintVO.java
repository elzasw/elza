package cz.tacr.elza.controller.vo;

/**
 * VO omezení hodnoty atributu
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class RulDescItemConstraintVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * kód
     */
    private String code;

    /**
     * identifikátor typu atributu
     */
    private Integer descItemTypeId;

    /**
     * identifikátor specifikace atributu
     */
    private Integer descItemSpecId;

    /**
     * identifikátor verze archivní pomůcky
     */
    private Integer fundVersionId;

    /**
     * je opakovatelný?
     */
    private Boolean repeatable;

    /**
     * regulární výraz
     */
    private String regexp;

    /**
     * maximální délka textu
     */
    private Integer textLenghtLimit;


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

    public Integer getDescItemTypeId() {
        return descItemTypeId;
    }

    public void setDescItemTypeId(final Integer descItemTypeId) {
        this.descItemTypeId = descItemTypeId;
    }

    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    public Integer getTextLenghtLimit() {
        return textLenghtLimit;
    }

    public void setTextLenghtLimit(final Integer textLenghtLimit) {
        this.textLenghtLimit = textLenghtLimit;
    }
}
