package cz.tacr.elza.service.arrangement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

public class MultiplItemChangeContext implements BatchChangeContext {

    final private RuleService ruleService;
    final private EventNotificationService notificationService;

    /**
     * Maximum number of pending changes
     */
    int maxItemCount = 1000;

    /**
     * Counter for modified items
     */
    int modifiedItems = 0;

    final private Integer fundVersionId;

    protected final CachedNodeRepository cacheNodeRepository;

    Set<Integer> nodeIds = new HashSet<>();

    List<ArrDescItem> createdItems = new ArrayList<>();

    List<ArrDescItem> updatedItems = new ArrayList<>();

    /**
     * Flag if changes should be published per item or per node
     */
    boolean publishNodeChanges = true;

    public void setPublishNodeChanges(final boolean publishNodeChanges) {
        this.publishNodeChanges = publishNodeChanges;
    }

    public MultiplItemChangeContext(final CachedNodeRepository cacheNodeRepository,
                                    final RuleService ruleService,
                                    final EventNotificationService notificationService,
                                    final Integer fundVersionId) {
        this.cacheNodeRepository = cacheNodeRepository;
        this.ruleService = ruleService;
        this.notificationService = notificationService;
        this.fundVersionId = fundVersionId;
    }

    @Override
    public void addCreatedItem(ArrDescItem descItemCreated) {
        createdItems.add(descItemCreated);
        nodeIds.add(descItemCreated.getNodeId());
        modifiedItems++;
    }

    @Override
    public void addUpdatedItem(ArrDescItem descItemUpdated) {
        updatedItems.add(descItemUpdated);
        nodeIds.add(descItemUpdated.getNodeId());
        modifiedItems++;
    }

    @Override
    public boolean getFlushNodeCache() {
        return false;
    }

    public void flush() {
        cacheNodeRepository.flush();

        // check is some node was modified
        if (nodeIds.size() == 0) {
            return;
        }
        ruleService.conformityInfo(fundVersionId,
                                   nodeIds,
                                   NodeTypeOperation.SAVE_DESC_ITEM,
                                   createdItems,
                                   updatedItems, null);

        if (publishNodeChanges) {
            Integer nodeIdsArray[] = nodeIds.toArray(new Integer[0]);

            EventIdsInVersion event = new EventIdsInVersion(EventType.NODES_CHANGE, fundVersionId,
                        nodeIdsArray);
            notificationService.publishEvent(event);
        } else {
            for (ArrDescItem createdItem : createdItems) {
                // sockety        
                EventChangeDescItem event = new EventChangeDescItem(EventType.DESC_ITEM_CHANGE, fundVersionId,
                        createdItem.getDescItemObjectId(),
                        createdItem.getNodeId(),
                        createdItem.getNode().getVersion());
                notificationService.publishEvent(event);
            }

            for (ArrDescItem updatedItem : updatedItems) {
                // sockety        
                EventChangeDescItem event = new EventChangeDescItem(EventType.DESC_ITEM_CHANGE, fundVersionId,
                        updatedItem.getDescItemObjectId(),
                        updatedItem.getNodeId(),
                        updatedItem.getNode().getVersion());
                notificationService.publishEvent(event);
            }
        }

        createdItems.clear();
        updatedItems.clear();
        nodeIds.clear();
        modifiedItems = 0;
    }

    public void flushIfNeeded() {
        if (modifiedItems > maxItemCount) {
            flush();
        }
    }

}
