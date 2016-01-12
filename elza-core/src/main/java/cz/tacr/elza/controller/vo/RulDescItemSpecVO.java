package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * VO specifikace atributu.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
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
     * seznam omezení
     */
    private List<RulDescItemConstraintVO> descItemConstraints;

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

    public List<RulDescItemConstraintVO> getDescItemConstraints() {
        return descItemConstraints;
    }

    public void setDescItemConstraints(final List<RulDescItemConstraintVO> descItemConstraints) {
        this.descItemConstraints = descItemConstraints;
    }
}
