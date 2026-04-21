package cz.tacr.elza.bulkaction;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.service.OutputServiceInternal;
import cz.tacr.elza.service.UserService;

@Component
@Scope("prototype")
public class AsyncBulkActionWorker implements IAsyncWorker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncBulkActionWorker.class);

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    protected BulkActionHelperService bulkActionHelperService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserService userService;

    /**
     * Hromadná akce. Nastavuje se v {@link #run()} přes {@link #setBulkAction},
     * čte se přes {@link #getBulkAction}. Obě metody jsou {@code synchronized},
     * takže pole nemusí být {@code volatile} — vzájemná exkluzivita dává
     * happens-before garanci mezi zapsáním v {@code run()} a přečtením v
     * {@link #terminate()} nebo {@link #toString()}.
     */
    private BulkAction bulkAction;

    /**
     * Příznak, že byl zavolán {@link #terminate()}. Guarded by {@code this}.
     * Pokud je {@code true} v okamžiku, kdy {@link #setBulkAction} dorazí,
     * setter spustí {@code terminate()} na nově přiřazené akci, aby
     * {@code run()} detekoval přerušení co nejdřív.
     */
    private boolean terminationRequested;

    private Long beginTime;

    private final AsyncRequest request;

    /**
     * Seznam vstupních uzlů (podstromů AS)
     */
    private List<Integer> inputNodeIds;

    public AsyncBulkActionWorker(final List<AsyncRequest> requests) {
        if (CollectionUtils.isNotEmpty(requests)) {
            Validate.isTrue(requests.size() == 1, "Only single request processing is supported by this worker");
            this.request = requests.get(0);
        } else {
            this.request = null;
        }
    }

    @Override
    public AsyncRequest getRequest() {
        return request;
    }

    @Override
    public List<AsyncRequest> getRequests() {
        return Collections.singletonList(request);
    }

    @Override
    public void run() {
        boolean success = false;
        Throwable failure = null;

        // Prepare action
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                beginTime = System.currentTimeMillis();
                ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
                setBulkAction(bulkActionHelperService.prepareToRun(bulkActionRun));
                inputNodeIds = bulkActionHelperService.getBulkActionNodeIds(bulkActionRun);
                logger.info("Bulk action started: {}", this);

                // start action - mark it as running
                bulkActionRun.setDateStarted(new Date());
                bulkActionRun.setState(ArrBulkActionRun.State.RUNNING);
                bulkActionHelperService.updateAction(bulkActionRun);
            });
        } catch (Throwable e) {
            logger.error("Failed to start action: {}", this, e);
            failure = e;
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    handleBulkActionError(e);
                });
            } catch (Exception ex) {
                logger.error("Failed to persist bulk action error state", ex);
            }
            // Notify executor and return — action was never started
            try {
                eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, failure));
            } catch (Exception ex) {
                logger.error("Failed to publish async request event", ex);
            }
            return;
        }

        // prepare sec context
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        // Run action
        try {
        	executeAction();
        	success = true;
        } catch (Throwable e) {
            logger.error("Bulk action failed, action: " + this + ", error: ", e);
            failure = e;
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    handleBulkActionError(e);
                });
            } catch (Exception ex) {
                logger.error("Failed to persist bulk action error state", ex);
            }
        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
            // Always notify executor so worker is removed from processing
            try {
                if (success) {
                    eventPublisher.publishEvent(AsyncRequestEvent.success(request, this));
                } else {
                    eventPublisher.publishEvent(AsyncRequestEvent.fail(request, this, failure));
                }
            } catch (Exception e) {
                logger.error("Failed to publish async request event", e);
            }
        }
    }

	private void executeAction() throws InterruptedException {
        // set active user
        ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
        SecurityContext ctx = userService.createSecurityContext(bulkActionRun.getUserId());
        SecurityContextHolder.setContext(ctx);

        try {
            // prepare context object
        	ActionRunContext runContext = new TransactionTemplate(transactionManager).execute(status -> {
            	return new ActionRunContext(inputNodeIds, bulkActionRun);
            });

            getBulkAction().execute(runContext);

            // TODO: Add check that action was not interrupted
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            	bulkActionRun.setDateFinished(new Date());
            	bulkActionRun.setState(ArrBulkActionRun.State.FINISHED);
            	bulkActionHelperService.updateAction(bulkActionRun);
            	bulkActionHelperService.onFinished(bulkActionRun);
            });

        } finally {
            // success/fail event is published by run() in its finally block
        }
        logger.info("Bulk action succesfully finished: {}", this);
    }

    @Override
    public Long getBeginTime() {
        return beginTime;
    }

    @Override
    public Long getRunningTime() {
        if (beginTime != null) {
            return System.currentTimeMillis() - beginTime;
        } else {
            return null;
        }
    }

    /**
     * Persist error state on the bulk action entity. Must be called in transaction.
     */
    private void handleBulkActionError(Throwable e) {
    	State actionState = ArrBulkActionRun.State.ERROR;
    	if (e instanceof InterruptedException || e instanceof BulkActionInterruptedException) {
    		actionState = ArrBulkActionRun.State.INTERRUPTED;
            logger.info("Bulk action interrupted: {}", this);
    	}
        ArrBulkActionRun bulkActionRun = bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId());
        // Build message
        String errorMsg;
        if(e instanceof AbstractException) {
        	errorMsg = e.toString();
        } else {
        	errorMsg = e.getLocalizedMessage();
        }
        bulkActionRun.setError(errorMsg);
        bulkActionRun.setState(actionState);

        // protože hromadná akce skončila chybou vrátíme výstup do původního stavu
        List<Integer> nodeIds = bulkActionHelperService.getBulkActionNodeIds(bulkActionRun);
        outputServiceInternal.changeOutputsStateByNodes(bulkActionRun.getFundVersion(),
                                                        nodeIds,
                                                        ArrOutput.OutputState.OPEN,
                                                        ArrOutput.OutputState.COMPUTING);

        bulkActionHelperService.updateAction(bulkActionRun);
    }

    /**
     * Přiřadí instanci {@link BulkAction} získanou z DB během {@link #run()}.
     * Pokud už mezitím někdo zavolal {@link #terminate()}, volá se
     * {@code action.terminate()} rovnou zde, aby běžící {@code run()} detekoval
     * přerušení co nejdřív (jinak by {@code run()} dál zpracovával akci,
     * kterou volající považuje za přerušenou).
     */
    private synchronized void setBulkAction(BulkAction action) {
        this.bulkAction = action;
        if (terminationRequested && action != null) {
            action.terminate();
        }
    }

    /**
     * Přečte aktuální instanci {@link BulkAction} (může být {@code null},
     * pokud {@link #run()} ji ještě nepřiřadil).
     */
    private synchronized BulkAction getBulkAction() {
        return bulkAction;
    }

    /**
     * Atomicky označí worker jako požadovaný k přerušení a vrátí aktuální
     * {@link BulkAction} (nebo {@code null}, pokud {@link #run()} ji ještě
     * nestihl přiřadit). Volající pak zavolá {@code terminate()} na vrácené
     * instanci. Pokud je vráceno {@code null}, o přerušení se postará
     * {@link #setBulkAction}, až jej {@code run()} zavolá.
     */
    private synchronized BulkAction requestTerminationAndSnapshot() {
        terminationRequested = true;
        return bulkAction;
    }

    /**
     * Ukončí běžící hromadnou akci.
     *
     * <p>Pole {@link #bulkAction} může být {@code null}, pokud byl worker
     * přidán do seznamu {@code processing}, ale {@link #run()} ještě nestihl
     * přiřadit instanci. V tom případě nastavíme jen {@code terminationRequested}
     * a skutečné přerušení proběhne v {@link #setBulkAction}, jakmile
     * {@code run()} projde přes {@code prepareToRun(...)}. Následně počkáme,
     * až se stav v DB přestane hlásit jako {@code RUNNING}, a označíme akci
     * jako {@code INTERRUPTED}.
     */
    public void terminate() {
        BulkAction action = requestTerminationAndSnapshot();
        if (action != null) {
            action.terminate();
        } else {
            logger.info("terminate() called before bulkAction was initialized for request {}; run() will pick up the termination flag", request.getRequestId());
        }

        ArrBulkActionRun bulkActionRun = null;
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            do {
                try {
                    bulkActionRun = tt.execute(status -> bulkActionHelperService.getArrBulkActionRun(request.getBulkActionId()));
                    logger.info("Waiting for bulkAction to complete: {}", bulkActionRun.getBulkActionCode());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Nothing to do with this -> simply finish
                    Thread.currentThread().interrupt();
                }
            } while (bulkActionRun.getState() == ArrBulkActionRun.State.RUNNING);

            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);

        } finally {
            bulkActionHelperService.updateAction(bulkActionRun);
        }
    }

    @Override
    public String toString() {
        BulkAction action = getBulkAction();
        return "BulkActionWorker{" +
                "bulkAction=" + (action != null ? action.getName() : "<not-yet-started>") +
                ", versionId=" + request.getFundVersionId() +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncBulkActionWorker that = (AsyncBulkActionWorker) o;
        return request.equals(that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request);
    }
}
