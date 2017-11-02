package cz.tacr.elza.service.cache;

import java.util.List;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNodeRegister;

/**
 * Objekt pro serializaci dat s informacemi o JP - pro NodeCacheService.
 *
 */
public class CachedNode implements NodeCacheSerializable {

    public CachedNode() {
    }

    public CachedNode(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Identifikátor JP.
     */
    private Integer nodeId;

    /**
     * Seznam hodnot atributů.
     */
    private List<ArrDescItem> descItems;

    /**
     * Seznam návazných entity z rejstříků.
     */
    private List<ArrNodeRegister> nodeRegisters;

    /**
     * Seznam navázaných entity z digitalizátů.
     */
    private List<ArrDaoLink> daoLinks;

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

    public List<ArrNodeRegister> getNodeRegisters() {
        return nodeRegisters;
    }

    public void setNodeRegisters(final List<ArrNodeRegister> nodeRegisters) {
        this.nodeRegisters = nodeRegisters;
    }

    public List<ArrDaoLink> getDaoLinks() {
        return daoLinks;
    }

    public void setDaoLinks(final List<ArrDaoLink> daoLinks) {
        this.daoLinks = daoLinks;
    }

    @Override
    public String toString() {
        return "CachedNode{" +
                "nodeId=" + nodeId +
                '}';
    }
}
