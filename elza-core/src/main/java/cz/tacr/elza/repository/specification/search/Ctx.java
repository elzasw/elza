package cz.tacr.elza.repository.specification.search;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

public class Ctx {

    public final CriteriaBuilder cb;
    public final CriteriaQuery<?> q;
    private Root<ApState> stateRoot;
    private Join<ApState, ApAccessPoint> accessPointJoin;
    private Join<ApAccessPoint, ApPart> preferredPartJoin;
    private Join<ApPart, ApIndex> apIndexRoot;
    private Join<ApAccessPoint, ApItem> apItemRoot;
    private Join<ApAccessPoint, ApPart> apPartRoot;
    private Join<RulItemType, ApItem> itemTypeJoin;
    private Join<RulItemSpec, ApItem> itemSpecJoin;
    private Join<RulPartType, ApPart> partTypeJoin;
    private Join<ApItem, ApPart> itemPartJoin;
    private Join<ApIndex, ApPart> indexPartJoin;

    public Ctx(final CriteriaBuilder cb, final CriteriaQuery<?> q) {
        this.cb = cb;
        this.q = q;
    }

    public void resetApItemRoot() {
        apItemRoot = null;
        itemTypeJoin = null;
        itemSpecJoin = null;
        itemPartJoin = null;
        apPartRoot = null;
        partTypeJoin = null;
    }

    public void resetApIndexRoot() {
        apIndexRoot = null;
        indexPartJoin = null;
        apPartRoot = null;
        partTypeJoin = null;
    }

    public Join<ApAccessPoint, ApItem> getApItemRoot() {
        if (apItemRoot == null) {
            apItemRoot = getApPartRoot().join(ApPart.ITEMS);
        }
        return apItemRoot;
    }

    public Join<ApPart, ApIndex> getApIndexRoot() {
        if (apIndexRoot == null) {
            apIndexRoot = getApPartRoot().join(ApPart.INDICES);
        }
        return apIndexRoot;
    }

    public Join<ApAccessPoint, ApPart> getApPartRoot() {
        if (apPartRoot == null) {
            apPartRoot = getAccessPointJoin().join(ApAccessPoint.PARTS);
        }
        return apPartRoot;
    }

    public Join<ApIndex, ApPart> getIndexPartJoin() {
        if (indexPartJoin  == null) {
            indexPartJoin = getApIndexRoot().join(ApIndex.PART, JoinType.INNER);
        }
        return indexPartJoin;
    }

    public Join<ApItem, ApPart> getItemPartJoin() {
        if (itemPartJoin == null) {
            itemPartJoin = getApItemRoot().join(ApItem.PART, JoinType.INNER);
        }
        return itemPartJoin;
    }

    public Join<RulItemType, ApItem> getItemTypeJoin() {
        if (itemTypeJoin == null) {
            itemTypeJoin = getApItemRoot().join(ApItem.ITEM_TYPE, JoinType.INNER);
        }
        return itemTypeJoin;
    }

    public Join<RulItemSpec, ApItem> getItemSpecJoin() {
        if (itemSpecJoin == null) {
            itemSpecJoin = getApItemRoot().join(ApItem.ITEM_SPEC, JoinType.LEFT);
        }
        return itemSpecJoin;
    }

    public Join<RulPartType, ApPart> getPartTypeJoin() {
        if (partTypeJoin == null) {
            partTypeJoin = getApPartRoot().join(ApPart.PART_TYPE, JoinType.INNER);
        }
        return partTypeJoin;
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
