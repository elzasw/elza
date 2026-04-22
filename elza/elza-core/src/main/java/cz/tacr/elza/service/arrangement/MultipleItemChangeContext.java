package cz.tacr.elza.service.arrangement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

@Component
@Scope("prototype")
public class MultipleItemChangeContext implements BatchChangeContext {

    final private DescItemRepository descItemRepository;

    final private RuleService ruleService;

    final private EventNotificationService notificationService;

    final private NodeCacheService nodeCacheService;

    private static class NodeChanges {

        List<ArrDescItem> createdItems = new ArrayList<>();

        List<ArrDescItem> updatedItems = new ArrayList<>();

        List<ArrDescItem> deletedItems = new ArrayList<>();

        public void addCreatedItem(ArrDescItem item) {
            createdItems.add(item);
        }

        public void addUpdatedItem(ArrDescItem descItemUpdated) {
            updatedItems.add(descItemUpdated);
        }

        public void addRemovedItem(ArrDescItem item) {
            deletedItems.add(item);
        }

        public List<ArrDescItem> getCreatedItems() {
            return createdItems;
        }

        public List<ArrDescItem> getUpdatedItems() {
            return updatedItems;
        }

        public List<ArrDescItem> getDeletedItems() {
            return deletedItems;
        }

        public void sendEventsPerChange(Integer fundVersionId, EventNotificationService notificationService) {
            for (ArrDescItem createdItem : createdItems) {
                // sockety        
                EventChangeDescItem event = new EventChangeDescItem(fundVersionId,
                        createdItem.getDescItemObjectId(),
                        createdItem.getNodeId(),
                        createdItem.getNode().getVersion());
                notificationService.publishEvent(event);
            }

            for (ArrDescItem updatedItem : updatedItems) {
                // sockety        
                EventChangeDescItem event = new EventChangeDescItem(fundVersionId,
                        updatedItem.getDescItemObjectId(),
                        updatedItem.getNodeId(),
                        updatedItem.getNode().getVersion());
                notificationService.publishEvent(event);
            }
            for (ArrDescItem item : deletedItems) {
                // sockety        
                EventChangeDescItem event = new EventChangeDescItem(fundVersionId,
                        item.getDescItemObjectId(),
                        item.getNodeId(),
                        item.getNode().getVersion());
                notificationService.publishEvent(event);
            }
        }
    }

    /**
     * Maximum number of pending changes
     */
    int maxItemCount = 1000;

    /**
     * Counter for modified items
     */
    int modifiedItems = 0;

    final private Integer fundVersionId;

    Map<Integer, NodeChanges> nodes = new HashMap<>();

    /**
     * Flag if changes should be published per item or per node
     */
    boolean publishNodeChanges = true;

    /**
     * If the update NodeCache is delayed until all records are processed
     */
    final boolean nodeCacheSyncDelayed;
    
    public MultipleItemChangeContext(final Integer fundVersionId,
                                     final DescItemRepository descItemRepository,
                                     final NodeCacheService nodeCacheService,
                                     final RuleService ruleService,
                                     final EventNotificationService eventNotificationService,
                                     boolean nodeCacheSyncDelayed) {
        this.descItemRepository = descItemRepository;
        this.nodeCacheService = nodeCacheService;
        this.ruleService = ruleService;
        this.notificationService = eventNotificationService;
        this.fundVersionId = fundVersionId;
        this.nodeCacheSyncDelayed = nodeCacheSyncDelayed;
    }

    public void setPublishNodeChanges(final boolean publishNodeChanges) {
        this.publishNodeChanges = publishNodeChanges;
    }

    private NodeChanges getNodeChanges(Integer nodeId) {
        return nodes.computeIfAbsent(nodeId, id -> new NodeChanges());
    }

    @Override
    public void addCreatedItem(ArrDescItem descItemCreated) {
        NodeChanges changes = getNodeChanges(descItemCreated.getNodeId());
        changes.addCreatedItem(descItemCreated);
        modifiedItems++;
    }

    @Override
    public void addUpdatedItem(ArrDescItem descItemUpdated) {
        NodeChanges changes = getNodeChanges(descItemUpdated.getNodeId());
        changes.addUpdatedItem(descItemUpdated);
        modifiedItems++;
    }


    @Override
    public void addRemovedItem(ArrDescItem item) {
        NodeChanges changes = getNodeChanges(item.getNodeId());
        changes.addRemovedItem(item);
        modifiedItems++;
    }

    @Override
    public boolean getFlushNodeCache() {
        return false;
    }

	@Override
	public boolean isNodeCacheSyncDelayed() {
		return nodeCacheSyncDelayed;
	}

	private void updateNodeCache() {
        if (nodeCacheSyncDelayed) {
        	Map<Integer, RestoredNode> nodesMap = nodeCacheService.getNodes(nodes.keySet());
			for (RestoredNode cachedNode : nodesMap.values()) {
        		NodeChanges node = nodes.get(cachedNode.getNodeId());
        		Objects.requireNonNull(node);
				node.getCreatedItems().forEach(item -> cachedNode.addDescItem(item));
				node.getUpdatedItems().forEach(item -> {
					ArrDescItem findItem = null;
					for (ArrDescItem descItem : cachedNode.getDescItems()) {
						if (descItem.getDescItemObjectId().equals(item.getDescItemObjectId())) {
							findItem = descItem;
	                        break;
						}
					}
					Objects.requireNonNull(findItem, "Not found ArrDescItem in CachedNode for update.");
                    // replace data in cache node
					findItem.setData(item.getData());
				});
				node.getDeletedItems().forEach(item -> {
					throw new SystemException("The functionality is not implemented.");
				});
        	}
        	nodeCacheService.saveNodes(nodesMap.values(), true);
		}
	}

	public void flush() {
        List<ArrDescItem> createdItems, updatedItems, deletedItems;

        // update all NodeCache items
        updateNodeCache();

        nodeCacheService.flushChanges();
        descItemRepository.flush();

        // check is some node was modified
        if (nodes.size() == 0) {
            return;
        }
        Set<Integer> nodeIds = nodes.keySet();

        if (nodes.size() == 1) {
            NodeChanges nodeChanges = nodes.get(nodeIds.iterator().next());
            createdItems = nodeChanges.getCreatedItems();
            updatedItems = nodeChanges.getUpdatedItems();
            deletedItems = nodeChanges.getDeletedItems();
        } else {
            // copy items from all nodes
            createdItems = new ArrayList<>();
            updatedItems = new ArrayList<>();
            deletedItems = new ArrayList<>();

            for (NodeChanges nodeChange : nodes.values()) {
                createdItems.addAll(nodeChange.getCreatedItems());
                updatedItems.addAll(nodeChange.getUpdatedItems());
                deletedItems.addAll(nodeChange.getUpdatedItems());
            }
        }
        ruleService.conformityInfo(fundVersionId,
                                   nodeIds,
                                   NodeTypeOperation.SAVE_DESC_ITEM,
                                   createdItems, updatedItems, deletedItems);

        if (publishNodeChanges) {
            Integer nodeIdsArray[] = nodeIds.toArray(new Integer[0]);

            EventIdsInVersion event = new EventIdsInVersion(EventType.NODES_CHANGE, fundVersionId, nodeIdsArray);
            notificationService.publishEvent(event);
        } else {
            // publish event per change
            for (NodeChanges nodeChange : nodes.values()) {
                nodeChange.sendEventsPerChange(fundVersionId, notificationService);
            }
        }

        nodes.clear();
        modifiedItems = 0;
    }

    public void flushIfNeeded() {
        if (modifiedItems > maxItemCount) {
            flush();
        }
    }
}
