package cz.tacr.elza.bulkaction;

import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.AsyncRequestVO;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.AsyncTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Component
@Scope("prototype")
public class AsyncBulkActionWorker implements IAsyncWorker {
    private static final Logger logger = LoggerFactory.getLogger(AsyncBulkActionWorker.class);

    @Autowired
    protected BulkActionHelperService bulkActionHelperService;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Hromadná akce reprezentovaná v DB
     */
    private ArrBulkActionRun bulkActionRun;

    /**
     * Hromadná akce
     */
    private BulkAction bulkAction;

    /**
     * Identifikátor verze archivní pomůcky
     */
    private Integer fundVersionId;

    private Long beginTime;

    private Long requestId;

    private Integer bulkActionRunId;

    /**
     * Seznam vstupních uzlů (podstromů AS)
     */
    private List<Integer> inputNodeIds;

    public AsyncBulkActionWorker(Integer fundVersionId, Long requestId, Integer bulkActionRunId) {
        this.fundVersionId = fundVersionId;
        this.requestId = requestId;
        this.bulkActionRunId = bulkActionRunId;
    }

    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    @Transactional
    public void run() {
        beginTime = System.currentTimeMillis();
        bulkActionRun = bulkActionHelperService.getArrBulkActionRun(bulkActionRunId);
        bulkAction = bulkActionHelperService.prepareToRun(bulkActionRun);
        inputNodeIds = bulkActionHelperService.getBulkActionNodeIds(bulkActionRun);
        logger.info("Bulk action started: {}", this);

        // start action - mark it as running
        bulkActionRun.setDateStarted(new Date());
        bulkActionRun.setState(ArrBulkActionRun.State.RUNNING);
       bulkActionHelperService.updateAction(bulkActionRun);

        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                executeInTransaction();
                logger.info("Bulk action succesfully finished: {}", this);
                return null;
            });
        } catch (Exception e) {
            new TransactionTemplate(transactionManager).execute(status -> {
                handleException(e);
                logger.error("Bulk action failed, action: " + this + ", error: ", e);
                return null;
            });
        }
        //return this;
    }

    private void executeInTransaction() {
        // prepare sec context
        SecurityContext originalSecCtx = SecurityContextHolder.getContext();
        SecurityContext ctx = bulkActionHelperService.createSecurityContext(this.bulkActionRun);
        SecurityContextHolder.setContext(ctx);

        try {

            // prepare context object
            ActionRunContext runContext = new ActionRunContext(inputNodeIds, bulkActionRun);

            bulkAction.execute(runContext);

            //    Thread.sleep(60000); // PRO TESTOVÁNÍ A DALŠÍ VÝVOJ

            // TODO: Add check that action was not interrupted
            bulkActionRun.setDateFinished(new Date());
            bulkActionRun.setState(ArrBulkActionRun.State.FINISHED);
            bulkActionHelperService.updateAction(bulkActionRun);
            bulkActionHelperService.onFinished(bulkActionRun);

        } finally {
            SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
            if (emptyContext.equals(originalSecCtx)) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(originalSecCtx);
            }
            eventPublisher.publishEvent(new AsyncRequestEvent(resultEvent()));
        }
    }

    private AsyncRequestVO resultEvent() {
        AsyncRequestVO publish = new AsyncRequestVO();
        publish.setType(AsyncTypeEnum.BULK);
        publish.setFundVersionId(fundVersionId);
        publish.setRequestId(requestId);
        publish.setBulkActionId(bulkActionRunId);
        return publish;
    }

    @Override
    public Long getRequestId() {
        return requestId;
    }

    @Override
    public Long getBeginTime() {
        return beginTime;
    }

    @Override
    public Long getRunningTime() {
        if(beginTime != null) {
            return System.currentTimeMillis() - beginTime;
        } else {
            return null;
        }
    }

    /**
     * Vrací identifikátor verze archivní pomůcky.
     *
     * @return identifikátor verze archivní pomůcky
     */
    public Integer getVersionId() {
        return fundVersionId;
    }

    private void handleException(Exception e) {
        bulkActionRun.setError(e.getLocalizedMessage());
        bulkActionRun.setState(ArrBulkActionRun.State.ERROR);
        bulkActionHelperService.updateAction(bulkActionRun);
        eventPublisher.publishEvent(new AsyncRequestEvent(resultEvent()));
    }

    /**
     * Ukončí běžící hromadnou akci.
     */
    public void terminate() {
        if (!bulkActionRun.setInterrupted(true)) {
            return;
        }

        try {
            while (bulkActionRun.getState() == ArrBulkActionRun.State.RUNNING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Nothing to do with this -> simply finish
                    Thread.currentThread().interrupt();
                }
            }

            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);
            bulkActionHelperService.updateAction(bulkActionRun);

        } finally {
            bulkActionRun.setInterrupted(false);
        }
    }

    @Override
    public String toString() {
        return "BulkActionWorker{" +
                "bulkAction=" + bulkAction +
                ", versionId=" + fundVersionId +
                ", bulkActionRun=" + bulkActionRun +
                '}';
    }

    @Override
    public Integer getCurrentId() {
        return bulkActionRun.getBulkActionRunId();
    }
}
