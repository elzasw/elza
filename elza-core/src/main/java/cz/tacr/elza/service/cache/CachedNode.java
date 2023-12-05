package cz.tacr.elza.service.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.common.db.HibernateUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Objekt pro serializaci dat s informacemi o JP - pro NodeCacheService.
 *
 */
public class CachedNode implements NodeCacheSerializable {

    /**
     * Identifikátor JP.
     */
    protected Integer nodeId;

    /**
     * JP uuid.
     */
    protected String uuid;

    /**
     * Seznam hodnot atributů.
     */
    protected List<ArrDescItem> descItems;

    /**
     * Seznam návazných entity z definic řídících pravidel.
     */
    protected List<ArrNodeExtension> nodeExtensions;

    /**
     * Seznam navázaných entity z digitalizátů.
     */
    protected List<ArrDaoLink> daoLinks;

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

    public List<ArrNodeExtension> getNodeExtensions() {
        return nodeExtensions;
    }

    public void setNodeExtensions(final List<ArrNodeExtension> nodeExtensions) {
        this.nodeExtensions = nodeExtensions;
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
	public void addDescItems(Collection<ArrDescItem> items) {
		if (descItems == null) {
			descItems = new ArrayList<>(items.size());
		}
		descItems.addAll(items);
	}

	public void addDescItem(ArrDescItem item) {
		if (descItems == null) {
			descItems = new ArrayList<>();
		}
		descItems.add(item);
	}

	public void removeDescItems(Collection<? extends ArrItem> items) {
        if (descItems != null) {
            descItems.removeAll(items);
        }
	}

	public void addNodeExtensions(Collection<ArrNodeExtension> nodeExtensions) {
        if (this.nodeExtensions == null) {
            this.nodeExtensions = new ArrayList<>(nodeExtensions.size());
        }
        this.nodeExtensions.addAll(nodeExtensions);
	}

    public void addDaoLinks(Collection<ArrDaoLink> daoLinks) {
        if (this.daoLinks == null) {
            this.daoLinks = new ArrayList<>(daoLinks.size());
        }
        this.daoLinks.addAll(daoLinks);
    }

    /**
     * Validate node data
     *
     * Typically called before serialization
     */
    public void validate() {
        if (uuid == null) {
            throw new NullPointerException("uuid is null");
        }
        validateDescItems();
        validateNodeExtensions();
        validateDaoLinks();
    }

    private void validateDaoLinks() {
        if (daoLinks == null) {
            return;
        }
        for (ArrDaoLink daoLink : daoLinks) {
            Validate.notNull(daoLink.getCreateChange());
            Validate.notNull(daoLink.getCreateChangeId());
            // Deleted items should not be stored
            Validate.isTrue(daoLink.getDeleteChangeId() == null);
            Validate.notNull(daoLink.getDao());
            Validate.notNull(daoLink.getDaoLinkId());
            Validate.notNull(daoLink.getNode());
            Validate.notNull(daoLink.getNodeId());
        }
    }

    private void validateNodeExtensions() {
        if (nodeExtensions == null) {
            return;
        }

        for (ArrNodeExtension nodeExtension : nodeExtensions) {
            if (nodeExtension.getCreateChange() == null) {
                throw new NullPointerException("Missing createChange");
            }
            if (nodeExtension.getCreateChangeId() == null) {
                throw new NullPointerException("Missing createChangeId");
            }
            if(nodeExtension.getArrangementExtension()==null) {
                throw new NullPointerException("Missing arrangement extension");
            }
            if(nodeExtension.getArrangementExtensionId()==null) {
                throw new NullPointerException("Missing arrangement extension ID");
            }
        }
    }

    private void validateDescItems() {
        if (descItems == null) {
            return;
        }
        for (ArrItem descItem : descItems) {
            // changeId is not stored in CachedNode
            // consider to store it also
            if (descItem.getCreateChange() == null) {
                throw new NullPointerException("createChange is null");
            }
            if (descItem.getCreateChangeId() == null) {
                throw new NullPointerException("createChangeId is null");
            }
            // Deleted items cannot be stored in cache
            if (descItem.getDeleteChangeId() != null || descItem.getDeleteChange() != null) {
                throw new SystemException("Item is marked as deleted and cannot be placed in cache",
                        BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("itemId", descItem.getItemId());
            }
            Validate.notNull(descItem.getDescItemObjectId());
            Validate.notNull(descItem.getItemType());
            Validate.notNull(descItem.getItemTypeId());
            Validate.notNull(descItem.getPosition());

            ArrData data = HibernateUtils.unproxy(descItem.getData());
            if (data != null) {
                data.validate();
            }
        }
    }
}
