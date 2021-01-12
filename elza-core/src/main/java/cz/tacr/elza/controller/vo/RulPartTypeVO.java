package cz.tacr.elza.controller.vo;

public class RulPartTypeVO extends BaseCodeVo {

    private Integer childPartId;
    private Boolean repeatable;

    public Integer getChildPartId() {
        return childPartId;
    }

    public void setChildPartId(Integer childPartId) {
        this.childPartId = childPartId;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
