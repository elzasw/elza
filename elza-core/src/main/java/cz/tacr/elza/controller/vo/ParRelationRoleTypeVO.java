package cz.tacr.elza.controller.vo;

/**
 * VO Seznamu rolí entit ve vztahu.
 *
 * @since 21.12.2015
 */
public class ParRelationRoleTypeVO
        extends BaseCodeVo {

    private Boolean repeatable;

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
