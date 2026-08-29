package cz.tacr.elza.service.da;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.repository.DaAipActionRepository;
import cz.tacr.elza.websocket.UserEventPushService;

/**
 * Tells the user who asked for an action over AIPs how it is going.
 *
 * Two rules, both taken from {@link cz.tacr.elza.service.ai.AiRequestPushService}, which learned
 * them the hard way:
 *
 * <ul>
 * <li><b>Commit first, render after.</b> The snapshot is built from the committed rows, in a
 * transaction of its own, never inside the one that records the outcome. Drawing the result must
 * not be able to endanger the result.</li>
 * <li><b>Never throw.</b> A failure to render is logged and the push is dropped. The action is
 * unaffected and the client corrects itself on its next fetch.</li>
 * </ul>
 *
 * Nothing is pushed for an action nobody asked for - the synchronization has no user to tell.
 */
@Component
public class DaAipActionPushService {

    private static final Logger logger = LoggerFactory.getLogger(DaAipActionPushService.class);

    @Autowired
    private DaAipActionRepository actionRepository;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Autowired
    private UserEventPushService pushService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Pushes the action once the transaction that changed it has committed.
     *
     * Registered as an after-commit hook rather than sent straight away: the client is told about
     * a state it can read back, and a rolled back change is never announced. Outside a transaction
     * the change is already committed, so the push goes out immediately.
     */
    public void pushAfterCommit(final Integer actionId, final Integer userId) {
        if (actionId == null || userId == null) {
            return; // nobody asked for this action, so there is nobody to tell
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(actionId, userId);
                }
            });
        } else {
            push(actionId, userId);
        }
    }

    private void push(final Integer actionId, final Integer userId) {
        DaAipActionUpdateMessage message;
        try {
            message = transactionTemplate.execute(status -> {
                DaAipAction action = actionRepository.findById(actionId).orElse(null);
                return action == null ? null : new DaAipActionUpdateMessage(clientFactoryVO.createAipAction(action));
            });
        } catch (RuntimeException e) {
            logger.error("Snímek akce {} se nepodařilo sestavit; akce je v pořádku, zpráva se neodesílá",
                         actionId, e);
            return;
        }
        if (message != null) {
            pushService.push(userId, message);
        }
    }
}
