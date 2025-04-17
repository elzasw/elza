package cz.tacr.elza.drools.model;

import java.util.List;

import cz.tacr.elza.domain.ApState.StateApproval;

public class Ap {

    private Integer id;
    private String aeType;
    private List<Part> parts;
    private StateApproval stateApproval;

    public Ap(final Integer id, final String aeType, final List<Part> parts, StateApproval stateApproval) {
        this.id = id;
        this.aeType = aeType;
        this.parts = parts;
        this.stateApproval = stateApproval;
    }

    public Integer getId() {
        return id;
    }

    public String getAeType() {
        return aeType;
    }

    public List<Part> getParts() {
        return parts;
    }
    
    public String getStateApproval() {
		return stateApproval.name();
	}
}
