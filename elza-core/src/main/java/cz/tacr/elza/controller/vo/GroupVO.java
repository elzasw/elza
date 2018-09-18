package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * VO pro reprezentaci skupiny typů atributů.
 *
 * @since 06.03.2018
 */
public class GroupVO {

    private String code;

    private String name;

    private List<TypeInfoVO> itemTypes;

    public GroupVO(final String code, final String name) {
        this.code = code;
        this.name = name;
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

    public List<TypeInfoVO> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(final List<TypeInfoVO> itemTypes) {
        this.itemTypes = itemTypes;
    }
}
