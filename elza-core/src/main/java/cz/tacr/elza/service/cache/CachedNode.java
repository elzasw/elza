package cz.tacr.elza.service.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNodeRegister;

/**
 * Objekt pro serializaci dat s informacemi o JP - pro NodeCacheService.
 *
 */
public class CachedNode implements NodeCacheSerializable {

    /**
     * Identifikátor JP.
     */
    private Integer nodeId;

    /**
     * JP uuid.
     */
    private String uuid;

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

    public CachedNode() {
    }

    public CachedNode(final Integer nodeId, final String uuid) {
        this.nodeId = nodeId;
        this.uuid = uuid;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
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

	/**
	 * Add description items to the node
	 *
	 * @param newDescItems
	 */
	public void addDescItems(Collection<ArrDescItem> newDescItems) {
		if (descItems == null) {
			descItems = new ArrayList<>(newDescItems.size());
		}
		descItems.addAll(newDescItems);
	}

	public void addDescItem(ArrDescItem descItem) {
		if (descItems == null) {
			descItems = new ArrayList<>();
		}
		descItems.add(descItem);
	}
}
