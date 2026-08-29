package cz.tacr.elza.service.da;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.DaAipActionItemRepository;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.UserService;

/**
 * Records the actions requested over AIPs and their outcome per AIP.
 *
 * An action is recorded whichever way it is carried out - by ELZA alone, or through the
 * synchronization queue of the digital archive - so the user is told about both the same way.
 *
 * Every method here works across transaction boundaries, because an action is carried out one AIP
 * per transaction: nothing is held over from one step to the next except identifiers, and the
 * entities are read again in the transaction that changes them.
 */
@Service
public class DaAipActionService {

    private static final Logger logger = LoggerFactory.getLogger(DaAipActionService.class);

    /**
     * The column holding the message is unbounded; the cap only keeps a pathological exception
     * message from bloating the row.
     */
    private static final int MESSAGE_MAX_LENGTH = 4000;

    @Autowired
    private DaAipActionRepository actionRepository;
    @Autowired
    private DaAipActionItemRepository actionItemRepository;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private DaAipActionPushService pushService;

    /**
     * Opens an action over the given AIPs, with every AIP outstanding.
     *
     * Called from an entry point that is not transactional, so this commits on its own before any
     * of the work starts and the items are readable as outstanding while it runs. It deliberately
     * joins a transaction when there is one - the AIPs it refers to may have been created in it.
     */
    @Transactional
    public DaAipAction start(DaAipActionType actionType, Collection<DaAip> aips) {
        DaAipAction action = new DaAipAction();
        action.setActionType(actionType);
        action.setUser(userService.getLoggedUser());
        action.setCreateDate(OffsetDateTime.now());
        actionRepository.save(action);

        for (DaAip aip : aips) {
            DaAipActionItem item = new DaAipActionItem();
            item.setAipAction(action);
            item.setAip(aip);
            item.setState(DaAipActionItemState.WAITING);
            action.getItems().add(item);
        }
        actionItemRepository.saveAll(action.getItems());
        return action;
    }

    /**
     * Sink recording into the given action, or {@link AipOutcomeSink#NONE} when there is no
     * action to record into.
     */
    public AipOutcomeSink sinkFor(@Nullable DaAipAction action) {
        if (action == null) {
            return AipOutcomeSink.NONE;
        }
        return sinkOver(itemIdsByAip(action.getItems()));
    }

    /**
     * Sink recording into the action items the given queue items are carrying out. Lets the
     * processing that follows a download report per AIP, before the batch as a whole is closed.
     */
    public AipOutcomeSink sinkForQueueItems(Collection<DaSyncQueueItem> queueItems) {
        if (queueItems == null) {
            return AipOutcomeSink.NONE;
        }
        return sinkOver(itemIdsByAip(queueItems.stream()
                .map(DaSyncQueueItem::getAipActionItem)
                .filter(java.util.Objects::nonNull)
                .toList()));
    }

    @Transactional(readOnly = true)
    public DaAipAction getAction(Integer actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new ObjectNotFoundException("Akce nad AIPy nenalezena, ID=" + actionId,
                                                              BaseCode.ID_NOT_EXIST).setId(actionId));
    }

    /**
     * Gives up on a step that was interrupted by a restart of the server.
     *
     * The step ran in a transaction of its own, so the restart rolled it back and nothing is half
     * written; the AIP can be run again. Doing so by itself is deliberately not offered - a step
     * that brings the server down would bring it down on every start - so the item is reported as
     * failed and the user decides whether to ask for it again. An item that had already finished
     * keeps its outcome.
     *
     * @return true when the step must not be started again, which is always: it is only called for
     *         requests left behind by a restart
     */
    @Transactional
    public boolean abandonInterruptedStep(@Nullable DaAipActionItem item) {
        if (item == null) {
            return true;
        }
        finishById(item.getAipActionItemId(), DaAipActionItemState.ERROR, "Byl proveden restart serveru");
        return true;
    }

    /**
     * Sink recording into one known item. Used by the worker, which is given the item to carry
     * out and has no action loaded.
     */
    public AipOutcomeSink sinkForItem(Integer actionItemId, Integer aipId) {
        return new ActionSink(Map.of(aipId, actionItemId));
    }

    /**
     * Records the outcome of one item in a transaction of its own. Used where the transaction the
     * work ran in is already rolled back and the outcome would be rolled back with it.
     */
    @Transactional
    public void recordOutcome(Integer actionItemId, DaAipActionItemState state, @Nullable String message) {
        finishById(actionItemId, state, message);
    }

    /**
     * Finishes the action items the queue items were carrying out, if they were carrying any.
     * Called from the processors, which know the outcome of the exchange with the digital archive.
     *
     * An item that is already done keeps its outcome: the processing that ran over the downloaded
     * package knows what happened to that one AIP, the batch only knows the exchange succeeded.
     */
    @Transactional
    public void completeFromQueue(Collection<DaSyncQueueItem> queueItems, DaAipActionItemState state,
                                  @Nullable String message) {
        if (queueItems == null) {
            return;
        }
        for (DaSyncQueueItem queueItem : queueItems) {
            DaAipActionItem item = queueItem.getAipActionItem();
            if (item != null) {
                finishById(item.getAipActionItemId(), state, message);
            }
        }
    }

    /**
     * State of the action, derived from its items: outstanding while any item is, failed when any
     * of them failed. It is derived rather than stored because the items are finished one per
     * transaction and a stored counter would have to be kept in step with them.
     */
    @Transactional(readOnly = true)
    public DaAipActionState stateOf(DaAipAction action) {
        return stateOf(actionItemRepository.findByAipActionOrderByAipActionItemId(action));
    }

    /** Package-private so the derivation itself can be tested without a database. */
    static DaAipActionState stateOf(List<DaAipActionItem> items) {
        if (items.isEmpty()) {
            return DaAipActionState.FINISHED;
        }
        boolean anyTerminal = false;
        boolean anyOutstanding = false;
        boolean anyFailed = false;
        for (DaAipActionItem item : items) {
            if (item.getState().isTerminal()) {
                anyTerminal = true;
                anyFailed |= item.getState() == DaAipActionItemState.ERROR;
            } else {
                anyOutstanding = true;
            }
        }
        if (anyOutstanding) {
            return anyTerminal ? DaAipActionState.RUNNING : DaAipActionState.WAITING;
        }
        return anyFailed ? DaAipActionState.ERROR : DaAipActionState.FINISHED;
    }

    private static Map<Integer, Integer> itemIdsByAip(Collection<DaAipActionItem> items) {
        Map<Integer, Integer> itemIdsByAip = new LinkedHashMap<>();
        for (DaAipActionItem item : items) {
            itemIdsByAip.put(item.getAip().getAipId(), item.getAipActionItemId());
        }
        return itemIdsByAip;
    }

    private AipOutcomeSink sinkOver(Map<Integer, Integer> itemIdsByAip) {
        return itemIdsByAip.isEmpty() ? AipOutcomeSink.NONE : new ActionSink(itemIdsByAip);
    }

    /**
     * Writes the outcome of one item. The item is read again here rather than held from an earlier
     * step, because the steps of an action run in separate transactions and an instance kept over
     * from a previous one is detached.
     */
    private void finishById(Integer itemId, DaAipActionItemState state, @Nullable String message) {
        DaAipActionItem item = actionItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            logger.debug("Položka akce ID={} už neexistuje", itemId);
            return;
        }
        if (item.getState().isTerminal()) {
            return;
        }
        item.setState(state);
        item.setMessage(StringUtils.abbreviate(message, MESSAGE_MAX_LENGTH));
        item.setFinishDate(OffsetDateTime.now());
        actionItemRepository.save(item);
        closeIfDone(item.getAipAction());
        announce(item.getAipAction());
    }

    /**
     * Stamps the action as finished once nothing is outstanding. The items are read from the
     * repository rather than from the action, because they are finished one per transaction and a
     * copy held in memory would not see the others. The collection of the action is deliberately
     * left alone - replacing it on a managed entity would detach the one Hibernate tracks.
     */
    /** Tells the user who asked for the action how it stands, once this transaction has committed. */
    private void announce(DaAipAction action) {
        pushService.pushAfterCommit(action.getAipActionId(),
                                    action.getUser() != null ? action.getUser().getUserId() : null);
    }

    private void closeIfDone(DaAipAction action) {
        List<DaAipActionItem> items = actionItemRepository.findByAipActionOrderByAipActionItemId(action);
        boolean done = items.stream().allMatch(i -> i.getState().isTerminal());
        if (done && action.getFinishDate() == null) {
            action.setFinishDate(OffsetDateTime.now());
            actionRepository.save(action);
            logger.debug("Akce {} nad {} AIPy dokončena", action.getActionType(), items.size());
        }
    }

    private class ActionSink implements AipOutcomeSink {

        private final Map<Integer, Integer> itemIdsByAip;

        ActionSink(Map<Integer, Integer> itemIdsByAip) {
            this.itemIdsByAip = itemIdsByAip;
        }

        @Override
        public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
            Integer itemId = itemIdsByAip.get(aipId);
            if (itemId == null) {
                // The processing reached an AIP the action was not opened over; recording it
                // would claim the user asked for something they did not.
                logger.debug("AIP={} není součástí akce", aipId);
                return;
            }
            finishById(itemId, state, message);
        }

        @Override
        public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
            Integer itemId = itemIdsByAip.get(aipId);
            if (itemId == null) {
                return;
            }
            DaAipActionItem item = actionItemRepository.findById(itemId).orElse(null);
            if (item == null) {
                return;
            }
            item.setState(DaAipActionItemState.RUNNING);
            actionItemRepository.save(item);
            queueItem.setAipActionItem(item);
            syncQueueItemRepository.save(queueItem);
            announce(item.getAipAction());
        }
    }
}
