package cz.tacr.elza.controller.vo;

import java.util.List;

public class SyncsFilterVO {

    private List<ExtAsyncQueueState> states = null;
    private List<String> scopes = null;

    public List<ExtAsyncQueueState> getStates() {
        return states;
    }

    public void setStates(final List<ExtAsyncQueueState> states) {
        this.states = states;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(final List<String> scopes) {
        this.scopes = scopes;
    }
}
