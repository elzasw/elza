package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApStateApproval;

public class ApStateChangeVO {

    private String comment;
    private ApStateApproval state;
    private Integer scopeId;
    private Integer typeId;

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public ApStateApproval getState() {
        return state;
    }

    public void setState(final ApStateApproval state) {
        this.state = state;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }
}
