package cz.tacr.elza.controller.vo;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.api.RulItemSpec;


/**
 * VO specifikace atributu.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class RulDescItemSpecVO {

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
     * zkratka
     */
    private String shortcut;

    /**
     * popis
     */
    private String description;

    /**
     * řazení
     */
    private Integer viewOrder;

    /**
     * typ důležitosti
     */
    @Deprecated
    private RulItemSpec.Type type;

    /**
     * opakovatelnost
     */
    @Deprecated
    private Boolean repeatable;

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

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public RulItemSpec.Type getType() {
        return type;
    }

    public void setType(final RulItemSpec.Type type) {
        this.type = type;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
