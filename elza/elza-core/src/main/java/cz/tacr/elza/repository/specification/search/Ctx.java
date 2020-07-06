package cz.tacr.elza.repository.specification.search;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

public class Ctx {

    public final CriteriaBuilder cb;
    public final CriteriaQuery<?> q;
    private Root<ApState> stateRoot;
    private Join<ApState, ApAccessPoint> accessPointJoin;
    private Join<ApAccessPoint, ApPart> preferredPartJoin;
    private Join<ApAccessPoint, ApItem> apItemRoot;
    private Join<ApAccessPoint, ApPart> apPartRoot;

    public Ctx(final CriteriaBuilder cb, final CriteriaQuery<?> q) {
        this.cb = cb;
        this.q = q;
    }

    public void resetApItemRoot() {
        apItemRoot = null;
    }

    public Join<ApAccessPoint, ApItem> getApItemRoot() {
        if (apItemRoot == null) {
            apItemRoot = getAePartRoot().join(ApPart.ITEMS);
        }
        return apItemRoot;
    }

    public Join<ApAccessPoint, ApPart> getAePartRoot() {
        if (apPartRoot == null) {
            apPartRoot = getAccessPointJoin().join(ApAccessPoint.PARTS);
        }
        return apPartRoot;
    }

    public Join<ApState, ApAccessPoint> getAccessPointJoin() {
        return accessPointJoin;
    }

    public void setAccessPointJoin(Join<ApState, ApAccessPoint> accessPointJoin) {
        this.accessPointJoin = accessPointJoin;
    }

    public Join<ApAccessPoint, ApPart> getPreferredPartJoin() {
        return preferredPartJoin;
    }

    public void setPreferredPartJoin(Join<ApAccessPoint, ApPart> preferredPartJoin) {
        this.preferredPartJoin = preferredPartJoin;
    }

    public Root<ApState> getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(Root<ApState> stateRoot) {
        this.stateRoot = stateRoot;
    }
}
