package cz.tacr.elza.controller.vo;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.RulItemSpec;


/**
 * VO specifikace atributu.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class RulDescItemSpecVO extends BaseCodeVo {
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

    public static RulDescItemSpecVO newInstance(final RulItemSpec itemSpec) {
    	RulDescItemSpecVO result = new RulDescItemSpecVO();
    	result.setId(itemSpec.getItemSpecId());
    	result.setName(itemSpec.getName());
    	result.setCode(itemSpec.getCode());
    	result.setShortcut(itemSpec.getShortcut());
    	result.setDescription(itemSpec.getDescription());
    	result.setViewOrder(itemSpec.getViewOrder());
    	result.setType(itemSpec.getType());
    	result.setRepeatable(itemSpec.getRepeatable());
    	return result;
    }
}
