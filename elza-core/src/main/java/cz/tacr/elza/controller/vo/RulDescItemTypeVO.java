package cz.tacr.elza.controller.vo;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.RulDescItemType;


/**
 * VO typu hodnoty atributu
 *
 * @author Martin Šlapa
 * @since 13.1.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class RulDescItemTypeVO {

    /**
     * identifikátor typu
     */
    private Integer id;

    /**
     * identifikátor datového typu
     */
    private Integer dataTypeId;

    /**
     * kód
     */
    private String code;

    /**
     * název
     */
    private String name;

    /**
     * zkratka
     */
    private String shortcut;

    /**
     * popis
     */
    private String description;

    /**
     * je hodnota unikátní?
     */
    private Boolean isValueUnique;

    /**
     * může se řadit?
     */
    private Boolean canBeOrdered;

    /**
     * použít specifikaci?
     */
    private Boolean useSpecification;

    /**
     * řazení ve formuláři jednotky popisu
     */
    private Integer viewOrder;

    /**
     * pouze pro archivní pomůcku
     */
    private Boolean faOnly;

    /**
     * typ důležitosti
     */
    private RulDescItemType.Type type;

    /**
     * opakovatelnost
     */
    private Boolean repeatable;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    public void setDataTypeId(final Integer dataTypeId) {
        this.dataTypeId = dataTypeId;
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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getIsValueUnique() {
        return isValueUnique;
    }

    public void setIsValueUnique(final Boolean isValueUnique) {
        this.isValueUnique = isValueUnique;
    }

    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    public Boolean getUseSpecification() {
        return useSpecification;
    }

    public void setUseSpecification(final Boolean useSpecification) {
        this.useSpecification = useSpecification;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public Boolean getFaOnly() {
        return faOnly;
    }

    public void setFaOnly(final Boolean faOnly) {
        this.faOnly = faOnly;
    }

    public RulDescItemType.Type getType() {
        return type;
    }

    public void setType(final RulDescItemType.Type type) {
        this.type = type;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
