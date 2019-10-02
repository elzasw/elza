package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.WfIssueState;

public class WfIssueStateVO extends BaseCodeVo {

    // --- fields ---

    /**
     * Příznak výchozího stavu
     */
    private boolean startState;

    /**
     * Příznak koncového stavu
     */
    private boolean finalState;

    // --- getters/setters ---

    public boolean isStartState() {
        return startState;
    }

    public void setStartState(boolean startState) {
        this.startState = startState;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public void setFinalState(boolean finalState) {
        this.finalState = finalState;
    }

    public static WfIssueStateVO newInstance(final WfIssueState issueState) {
        WfIssueStateVO result = new WfIssueStateVO();
        result.setId(issueState.getIssueStateId());
        result.setCode(issueState.getCode());
        result.setName(issueState.getName());
        result.setFinalState(issueState.isFinalState());
        result.setStartState(issueState.isStartState());
        return result;
    }
}
