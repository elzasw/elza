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

    /**
     * Opens an action over the given AIPs, with every AIP outstanding. The outcomes are recorded
     * through the sink of the action as the processing reaches them.
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
        Map<Integer, DaAipActionItem> itemsByAip = new LinkedHashMap<>();
        for (DaAipActionItem item : action.getItems()) {
            itemsByAip.put(item.getAip().getAipId(), item);
        }
        return new ActionSink(itemsByAip);
    }

    /**
     * Sink recording into the action items the given queue items are carrying out. Lets the
     * processing that follows a download report per AIP, before the batch as a whole is closed.
     */
    public AipOutcomeSink sinkForQueueItems(Collection<DaSyncQueueItem> queueItems) {
        Map<Integer, DaAipActionItem> itemsByAip = new LinkedHashMap<>();
        if (queueItems != null) {
            for (DaSyncQueueItem queueItem : queueItems) {
                DaAipActionItem item = queueItem.getAipActionItem();
                if (item != null) {
                    itemsByAip.put(item.getAip().getAipId(), item);
                }
            }
        }
        return itemsByAip.isEmpty() ? AipOutcomeSink.NONE : new ActionSink(itemsByAip);
    }

    /**
     * Finishes the action item the queue item was carrying out, if it was carrying one. Called
     * from the processors, which know the outcome of the exchange with the digital archive.
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
            if (item != null && !item.getState().isTerminal()) {
                finish(item, state, message);
            }
        }
    }

    /**
     * State of the action, derived from its items: outstanding while any item is, failed when any
     * of them failed. It is derived rather than stored because the items are finished
     * independently of each other and, once the actions run in parallel, on different threads.
     */
    public DaAipActionState stateOf(DaAipAction action) {
        List<DaAipActionItem> items = action.getItems();
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

    private void finish(DaAipActionItem item, DaAipActionItemState state, @Nullable String message) {
        item.setState(state);
        item.setMessage(StringUtils.abbreviate(message, MESSAGE_MAX_LENGTH));
        item.setFinishDate(OffsetDateTime.now());
        actionItemRepository.save(item);
        closeIfDone(item.getAipAction());
    }

    /**
     * Stamps the action as finished once nothing is outstanding. The items are read from the
     * repository rather than from the action, because an action carried out through the queue is
     * finished item by item, each in its own transaction, and a copy held in memory would not see
     * the others. The collection of the action is deliberately left alone - replacing it on a
     * managed entity would detach the one Hibernate tracks.
     */
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

        private final Map<Integer, DaAipActionItem> itemsByAip;

        ActionSink(Map<Integer, DaAipActionItem> itemsByAip) {
            this.itemsByAip = itemsByAip;
        }

        @Override
        public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
            DaAipActionItem item = itemsByAip.get(aipId);
            if (item == null) {
                // The processing reached an AIP the action was not opened over; recording it
                // would claim the user asked for something they did not.
                logger.debug("AIP={} není součástí akce", aipId);
                return;
            }
            finish(item, state, message);
        }

        @Override
        public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
            DaAipActionItem item = itemsByAip.get(aipId);
            if (item == null) {
                return;
            }
            item.setState(DaAipActionItemState.RUNNING);
            actionItemRepository.save(item);
            queueItem.setAipActionItem(item);
            syncQueueItemRepository.save(queueItem);
        }
    }
}
