package cz.tacr.elza.controller.vo;

public class IssueStateVO extends BaseCodeVo {

    // --- fields ---

    private boolean startState;

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
}
