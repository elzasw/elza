package cz.tacr.elza.service.arrangement;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;

/**
 * Zjednodusseny context
 *
 * Vhodny jen pro pridani nebo zmenu jednoho prvku popisu
 *
 * Nelze pouzit pro vymazani prvku popisu
 *
 */
public class SingleItemChangeContext implements BatchChangeContext {

    private Integer nodeId;
    final private Integer fundVersionId;
    final private RuleService ruleService;
    final private EventNotificationService notificationService;

    List<ArrDescItem> createdDescItems = null;
    List<ArrDescItem> updatedDescItems = null;
    Integer nodeVersion = null;
    Integer descItemObjectId = null;

    public SingleItemChangeContext(final RuleService ruleService,
                                   final EventNotificationService notificationService,
                                   final Integer fundVersionId, final Integer nodeId) {
        this.ruleService = ruleService;
        this.notificationService = notificationService;
        this.fundVersionId = fundVersionId;
        this.nodeId = nodeId;
    }

    @Override
    public void addCreatedItem(ArrDescItem descItemCreated) {
        Validate.isTrue(this.createdDescItems == null);
        Validate.isTrue(this.updatedDescItems == null);

        createdDescItems = Collections.singletonList(descItemCreated);
        nodeVersion = descItemCreated.getNode().getVersion();
        descItemObjectId = descItemCreated.getDescItemObjectId();
    }

    @Override
    public void addUpdatedItem(ArrDescItem descItemUpdated) {
        Validate.isTrue(this.createdDescItems == null);
        Validate.isTrue(this.updatedDescItems == null);

        updatedDescItems = Collections.singletonList(descItemUpdated);
        nodeVersion = descItemUpdated.getNode().getVersion();
        descItemObjectId = descItemUpdated.getDescItemObjectId();
    }
    public void validateAndPublish() {
        // validace uzlu
        int validationPriority = 10;
        ruleService.conformityInfo(fundVersionId, Collections.singletonList(nodeId),
                                   NodeTypeOperation.SAVE_DESC_ITEM, createdDescItems, updatedDescItems, null, validationPriority);
        // publish event over socket (lower priority)
        EventChangeDescItem event = new EventChangeDescItem(fundVersionId,
                descItemObjectId,
                nodeId, nodeVersion);
        notificationService.publishEvent(event);
    }

    @Override
    public boolean getFlushNodeCache() {
        return true;
    }

    @Override
    public void addRemovedItem(ArrDescItem item) {
        throw new IllegalStateException("Unsupported operation");

    }

}
