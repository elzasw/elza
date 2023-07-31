package cz.tacr.elza.domain.projection;

import java.time.OffsetDateTime;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.UsrUser;

public class ApStateInfo {

    // --- fields ---

    private final OffsetDateTime changeDate;
    private final StateApproval state;
    private final RevStateApproval revState;
    private final String scopeName;
    private final String typeName;
    private final String revTypeName;
    private final String comment;
    private final String revComment;
    private final UsrUser user;

    // --- getters ---

    public OffsetDateTime getChangeDate() {
        return changeDate;
    }

    public StateApproval getState() {
        return state;
    }

    public RevStateApproval getRevState() {
        return revState;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getRevTypeName() {
        return revTypeName;
    }

    public String getComment() {
        return comment;
    }

    public String getRevComment() {
        return revComment;
    }

    public UsrUser getUser() {
        return user;
    }

    // --- constructor ---

    public ApStateInfo(OffsetDateTime changeDate, StateApproval state, RevStateApproval revState, String scopeName, 
                       String typeName, String revTypeName, String comment, String revComment, UsrUser user) {
        this.changeDate = changeDate;
        this.state = state;
        this.revState = revState;
        this.scopeName = scopeName;
        this.typeName = typeName;
        this.revTypeName = revTypeName;
        this.comment = comment;
        this.revComment = revComment;
        this.user = user;
    }
}
