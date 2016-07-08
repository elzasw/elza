package cz.tacr.elza.controller.vo;

/**
 * VO Seznamu rolí entit ve vztahu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParRelationRoleTypeVO {

    /**
     * Id.
     */
    private Integer roleTypeId;

    /**
     * Kod.
     */
    private String code;
    /**
     * Název.
     */
    private String name;

    public Integer getRoleTypeId() {
        return roleTypeId;
    }

    public void setRoleTypeId(final Integer roleTypeId) {
        this.roleTypeId = roleTypeId;
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
}
