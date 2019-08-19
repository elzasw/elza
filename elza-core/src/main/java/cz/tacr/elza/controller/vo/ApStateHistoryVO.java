package cz.tacr.elza.controller.vo;

import java.util.Date;

import cz.tacr.elza.domain.ApState;

public class ApStateHistoryVO {

    /**
     * Datum změny.
     */
    private Date changeDate;

    /**
     * Uživatelské jméno osoby, která změnu proveda.
     */
    private String username;

    /**
     * Název oblasti.
     */
    private String scope;

    /**
     * Typ přístupového bodu.
     */
    private String type;

    /**
     * Stav změny.
     */
    private ApState.StateApproval state;

    /**
     * Komentář změny.
     */
    private String comment;

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(final Date changeDate) {
        this.changeDate = changeDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public ApState.StateApproval getState() {
        return state;
    }

    public void setState(final ApState.StateApproval state) {
        this.state = state;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }
}
