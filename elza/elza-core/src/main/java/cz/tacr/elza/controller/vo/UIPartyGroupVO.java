package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.enums.UIPartyGroupTypeEnum;

/**
 * VO nastavení zobrazení formuláře pro osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 10. 2016
 */
public class UIPartyGroupVO {

    private Integer id;

    private ParPartyTypeVO partyType;

    private String code;

    private String name;

    private Integer viewOrder;

    private UIPartyGroupTypeEnum type;

    private String contentDefinition;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ParPartyTypeVO getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyTypeVO partyType) {
        this.partyType = partyType;
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

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public UIPartyGroupTypeEnum getType() {
        return type;
    }

    public void setType(final UIPartyGroupTypeEnum type) {
        this.type = type;
    }

    public String getContentDefinition() {
        return contentDefinition;
    }

    public void setContentDefinition(final String contentDefinition) {
        this.contentDefinition = contentDefinition;
    }

}
