package cz.tacr.elza.bulkaction;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;

/**
 * Úloha hromadné akce.
 *
 */
public class BulkActionWorker implements Callable<BulkActionWorker> {

	private static final Logger logger = LoggerFactory.getLogger(BulkActionWorker.class);

    /**
     * Hromadná akce reprezentovaná v DB
     */
    private final ArrBulkActionRun bulkActionRun;

    private final BulkActionService bulkActionService;

    private final PlatformTransactionManager transactionManager;

    /**
     * Hromadná akce
     */
    private BulkAction bulkAction;

    /**
     * Identifikátor verze archivní pomůcky
     */
    private Integer versionId;

    /**
     * Seznam vstupních uzlů (podstromů AS)
     */
    private List<Integer> inputNodeIds;

    public BulkActionWorker(final BulkAction bulkAction, final ArrBulkActionRun bulkActionRun,
            final List<Integer> inputNodeIds, final BulkActionService bulkActionService,
            final PlatformTransactionManager transactionManager) {
        this.bulkAction = bulkAction;
        this.bulkActionRun = Validate.notNull(bulkActionRun);
		this.bulkActionService = bulkActionService;
		this.transactionManager = transactionManager;
        this.inputNodeIds = inputNodeIds;

        this.versionId = bulkActionRun.getFundVersionId();
	}

    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    /**
     * Vrací identifikátor verze archivní pomůcky.
     *
     * @return identifikátor verze archivní pomůcky
     */
    public Integer getVersionId() {
        return versionId;
    }

    public void setStateAndPublish(State state) {
        bulkActionRun.setState(state);
        bulkActionService.storeBulkActionRun(bulkActionRun);
        bulkActionService.eventPublishBulkAction(bulkActionRun);
    }

    @Override
    public BulkActionWorker call() throws Exception {
        logger.info("Bulk action started: {}", this);

        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                executeInTransaction();
                logger.info("Bulk action succesfully finished: {}", this);
                return null;
            });
        } catch (Exception e) {
            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(e);
                logger.error("Bulk action failed, action: "+ this +", error: ", e);
                return null;
            });
        }
        return this;
    }

    private void executeInTransaction() {
        // prepare sec context
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        SecurityContext ctx = bulkActionService.createSecurityContext(this.bulkActionRun);
        SecurityContextHolder.setContext(ctx);
        
        try {
        
        bulkActionRun.setDateStarted(new Date());
        setStateAndPublish(State.RUNNING);

        //bulkActionRun.setChange(createChange(userId));
        bulkActionService.storeBulkActionRun(bulkActionRun);
        // prepare context object
        ActionRunContext runContext = new ActionRunContext(inputNodeIds, bulkActionRun);

        bulkAction.execute(runContext);

        //Thread.sleep(30000); // PRO TESTOVÁNÍ A DALŠÍ VÝVOJ

        // TODO: Add check that action was not interrupted
        bulkActionRun.setDateFinished(new Date());
        setStateAndPublish(State.FINISHED);
        bulkActionService.onFinished(bulkActionRun);
        }
        finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if(emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }            
        }
    }

    private void handleException(Exception e) {
        bulkActionRun.setError(e.getLocalizedMessage());
        setStateAndPublish(State.ERROR);
    }

    /**
     * Ukončí běžící hromadnou akci.
     */
    public void terminate() {
        if (!bulkActionRun.setInterrupted(true)) {
            return;
        }

        try {
            while (bulkActionRun.getState() == State.RUNNING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Nothing to do with this -> simply finish
                    Thread.currentThread().interrupt();
                }
            }
            setStateAndPublish(State.INTERRUPTED);
        } finally {
            bulkActionRun.setInterrupted(false);
        }
    }

    @Override
    public String toString() {
        return "BulkActionWorker{" +
                "bulkAction=" + bulkAction +
                ", versionId=" + versionId +
                ", bulkActionRun=" + bulkActionRun +
                '}';
    }
}
