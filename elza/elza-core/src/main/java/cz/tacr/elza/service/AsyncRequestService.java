package cz.tacr.elza.service;

import static cz.tacr.elza.repository.ExceptionThrow.bulkAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.asynchactions.AsQueue;
import cz.tacr.elza.asynchactions.AsyncAccessPointWorker;
import cz.tacr.elza.asynchactions.AsyncExecutor;
import cz.tacr.elza.asynchactions.AsyncNodeWorker;
import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.AsyncWorkerVO;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.asynchactions.NodePriorityComparator;
import cz.tacr.elza.asynchactions.NodeQueuePriorityComparator;
import cz.tacr.elza.asynchactions.RequestQueue;
import cz.tacr.elza.bulkaction.AsyncBulkActionWorker;
import cz.tacr.elza.controller.vo.ArrAsyncRequestVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.FundStatisticsVO;
import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.AsyncTypeEnum;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.AsyncOutputGeneratorWorker;

/**
 * Servisní třída pro spouštění validací a hromadných akcí.
 */
@Service
@Configuration
@EnableAsync
public class AsyncRequestService implements ApplicationListener<AsyncRequestEvent> {

    static final Logger logger = LoggerFactory.getLogger(AsyncRequestService.class);

    @Value("${elza.asyncActions.node.maxPerFund:2}")
    @Min(1)
    @Max(100)
    private int nodeMaxPerFund;

    @Value("${elza.asyncActions.bulk.maxPerFund:1}")
    @Min(1)
    @Max(100)
    private int bulkMaxPerFund;

    @Value("${elza.asyncActions.output.maxPerFund:1}")
    @Min(1)
    @Max(100)
    private int outputMaxPerFund;

    @Autowired
    private ApplicationContext appCtx;

    @Autowired
    private ArrAsyncRequestRepository asyncRequestRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    @Qualifier(value = "threadPoolTaskExecutorAR")
    private ThreadPoolTaskExecutor nodeTaskExecutor;

    @Autowired
    @Qualifier("threadPoolTaskExecutorBA")
    private ThreadPoolTaskExecutor bulkActionTaskExecutor;

    @Autowired
    @Qualifier("threadPoolTaskExecutorOP")
    private ThreadPoolTaskExecutor outputTaskExecutor;

    @Autowired
    @Qualifier("threadPoolTaskExecutorAP")
    private ThreadPoolTaskExecutor accessPointTaskExecutor;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    private final AtomicBoolean runningService = new AtomicBoolean(false);
    private final Map<AsyncTypeEnum, AsyncExecutor> asyncExecutors = new HashMap<>();

    @PostConstruct
    protected void init() {
        register(new AsyncNodeExecutor(nodeTaskExecutor, txManager, asyncRequestRepository, appCtx, nodeMaxPerFund));
        register(new AsyncBulkExecutor(bulkActionTaskExecutor, txManager, asyncRequestRepository, appCtx, bulkMaxPerFund, bulkActionRepository));
        register(new AsyncOutputExecutor(outputTaskExecutor, txManager, asyncRequestRepository, appCtx, outputMaxPerFund, outputRepository));
        register(new AsyncAccessPointExecutor(accessPointTaskExecutor, txManager, asyncRequestRepository, appCtx));
    }

    private void register(final AsyncExecutor asyncExecutor) {
        asyncExecutors.put(asyncExecutor.getType(), asyncExecutor);
    }

    /**
     * Přídání výstupu do fronty na zpracování
     */
    @Transactional
    public void enqueue(ArrFundVersion fundVersion, ArrOutput output, Integer userId) {
        ArrAsyncRequest request = ArrAsyncRequest.create(fundVersion, output, 1, userId);
        asyncRequestRepository.save(request);
        getExecutor(request.getType()).enqueue(new AsyncRequest(request));
    }

    /**
     * Přidání hromadné akce do fronty na zpracování.
     */
    @Transactional
    public void enqueue(final ArrFundVersion fundVersion,
                        final ArrBulkActionRun bulkActionRun) {
        eventPublishBulkAction(bulkActionRun);
        ArrAsyncRequest request = ArrAsyncRequest.create(fundVersion, bulkActionRun, 1);
        asyncRequestRepository.save(request);
        getExecutor(request.getType()).enqueue(new AsyncRequest(request));
    }

    /**
     * Přidání JP do fronty ke zpracování s výchozí prioritou.
     */
    @Transactional
    public void enqueue(final ArrFundVersion fundVersion,
                        final List<ArrNode> nodeList) {
        enqueue(fundVersion, nodeList, null);
    }

    /**
     * Přidání JP do fronty ke zpracování.
     */
    @Transactional
    public void enqueue(final ArrFundVersion fundVersion,
                        final List<ArrNode> nodeList,
                        final Integer priority) {
        List<ArrAsyncRequest> dbReqList = new ArrayList<>(nodeList.size());
        for (ArrNode node : nodeList) {
            ArrAsyncRequest request = ArrAsyncRequest.create(fundVersion, node, priority == null ? 1 : priority);
            dbReqList.add(request);
        }
        Iterable<ArrAsyncRequest> saveReqList = asyncRequestRepository.saveAll(dbReqList);

        List<AsyncRequest> requests = new ArrayList<>(nodeList.size());
        for (ArrAsyncRequest request : saveReqList) {
            requests.add(new AsyncRequest(request));
        }
        getExecutor(AsyncTypeEnum.NODE).enqueue(requests);
    }

    @Transactional
    public void enqueue(final Collection<Integer> accessPointIds) {
        enqueue(accessPointIds, null);
    }

    @Transactional
    public void enqueue(final Collection<Integer> accessPointIds,
                        final Integer priority) {
        if (CollectionUtils.isNotEmpty(accessPointIds)) {
            List<AsyncRequest> requests = new ArrayList<>(accessPointIds.size());
            for (Integer accessPointId : accessPointIds) {
                ArrAsyncRequest request = ArrAsyncRequest.create(accessPointRepository.getOne(accessPointId), priority == null ? 1 : priority);
                asyncRequestRepository.save(request);
                requests.add(new AsyncRequest(request));
            }
            getExecutor(AsyncTypeEnum.AP).enqueue(requests);
        }
    }

    private AsyncExecutor getExecutor(final AsyncTypeEnum type) {
        return asyncExecutors.get(type);
    }

    /**
     * Kontrola, jestli běží validace AS před mazáním celého AS.
     */
    public boolean isFundNodeRunning(final ArrFundVersion version) {
        return getExecutor(AsyncTypeEnum.NODE).isProcessing(version.getFundVersionId());
    }

    /**
     * Kontrola, jestli běží hromadná akce na AS před mazáním celého AS.
     */
    public boolean isFundBulkActionRunning(final ArrFundVersion version) {
        return getExecutor(AsyncTypeEnum.BULK).isProcessing(version.getFundVersionId());
    }

    /**
     * Vytvoření statistické třídy pro AS, pokud neexistuje
     */
    private FundStatisticsVO createFundStatisticsVO(int fundVersionId) {
        ArrFundVersion version = fundVersionRepository.findByIdWithFetchFund(fundVersionId);
        ArrFundVO statFund = new ArrFundVO();
        statFund.setName(version.getFund().getName());
        statFund.setId(version.getFund().getFundId());
        statFund.setInstitutionId(version.getFund().getInstitution().getInstitutionId());
        return new FundStatisticsVO(fundVersionId, statFund);
    }

    /**
     * Přeruší všechny akce pro danou verzi. (všechny naplánované + čekající)
     * <p>
     * Synchronní metoda, čeká na přerušení
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void terminateBulkActions(final Integer fundVersionId) {
        getExecutor(AsyncTypeEnum.BULK).terminateFund(fundVersionId);
    }

    /**
     * Přeruší hromadnou akci pokud je ve stavu - čeká | plánování | běh
     *
     * @param bulkActionId Id hromadné akce
     */
    public void interruptBulkAction(final int bulkActionId) {
        ArrBulkActionRun bulkActionRun = bulkActionRepository.findById(bulkActionId)
                .orElseThrow(bulkAction(bulkActionId));

        ArrBulkActionRun.State originalState = bulkActionRun.getState();

        if(originalState.equals(ArrBulkActionRun.State.FINISHED)||
        		originalState.equals(ArrBulkActionRun.State.ERROR)||
        		originalState.equals(ArrBulkActionRun.State.INTERRUPTED)
        		) {
        	// action already finished
        	return;
        }

        if (!originalState.equals(ArrBulkActionRun.State.WAITING) &&
        		!originalState.equals(ArrBulkActionRun.State.PLANNED) &&
        		!originalState.equals(ArrBulkActionRun.State.RUNNING)) {
            throw new IllegalArgumentException("Nelze přerušit hromadnou akci ve stavu " + originalState + "!");
        }

        getExecutor(AsyncTypeEnum.BULK).terminate(bulkActionRun.getBulkActionRunId());
    }

    /**
     * Zastavení workerů podle verze AS.
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void terminateNodeWorkersByFund(final Integer fundVersionId) {
        getExecutor(AsyncTypeEnum.NODE).terminateFund(fundVersionId);
    }

    @Override
    public void onApplicationEvent(final AsyncRequestEvent event) {
        AsyncRequest request = event.getAsyncRequest();
        if (event.success()) {
            getExecutor(request.getType()).onSuccess(event.getWorker());
        } else {
            getExecutor(request.getType()).onFail(event.getWorker(), event.getError());
        }
    }

    /**
     * Vytváření statistiky pro zatížení.
     */
    @Scheduled(fixedDelay = 1000)
    protected void scheduledTask() {
        LocalDateTime now = LocalDateTime.now();
        int second = now.getSecond() + 60 * now.getMinute();
        asyncExecutors.values().forEach(s -> s.writeSlot(second));
    }

    /**
     * Vrácení detailních statistik podle jednotlivých typů požadavků.
     */
    public List<FundStatisticsVO> getFundStatistics(final AsyncTypeEnum type) {
        Map<Integer, FundStatisticsVO> map = new HashMap<>();
        AsyncExecutor asyncExecutor = getExecutor(type);
        asyncExecutor.doLockQueue(() -> {
            for (final AsyncRequest request : asyncExecutor.getCurrentRequests()) {
                Integer fundVersionId = request.getFundVersionId();
                if (fundVersionId != null) {
                    FundStatisticsVO fundStatistics = map.get(fundVersionId);
                    if (fundStatistics == null) {
                        fundStatistics = createFundStatisticsVO(fundVersionId);
                        map.put(fundVersionId, fundStatistics);
                    }
                    fundStatistics.addCount();
                }
            }
        });
        List<FundStatisticsVO> statistics = new ArrayList<>(map.values());
        statistics.sort(Collections.reverseOrder());
        return statistics.subList(0, Math.min(statistics.size(), 100));
    }

    private List<AsyncWorkerVO> convertWorkerList(Collection<IAsyncWorker> workers) {
        List<AsyncWorkerVO> runningVOList = new ArrayList<>();
        for (IAsyncWorker worker : workers) {
            AsyncRequest request = worker.getRequest();
            AsyncWorkerVO workerVO = new AsyncWorkerVO(request.getFundVersionId(), request.getRequestId(), worker.getBeginTime(), worker.getRunningTime(), request.getCurrentId());
            runningVOList.add(workerVO);
        }
        return runningVOList;
    }

    /**
     * Obecné informace o zpracování požadavků.
     */
    public List<ArrAsyncRequestVO> dispatcherInfo() {
        List<ArrAsyncRequestVO> infoList = new ArrayList<>();

        asyncExecutors.values().forEach(asyncExecutor -> {
            AtomicReference<List<AsyncWorkerVO>> workers = new AtomicReference<>();
            AtomicReference<Integer> waiting = new AtomicReference<>();
            AtomicReference<Integer> running = new AtomicReference<>();
            AtomicReference<Integer> requestCount = new AtomicReference<>();
            AtomicReference<Double> load = new AtomicReference<>();
            asyncExecutor.doLockQueue(() -> {
                load.set(asyncExecutor.getCurrentLoad());
                workers.set(convertWorkerList(asyncExecutor.getProcessing()));
                waiting.set(asyncExecutor.getQueueSize());
                running.set(asyncExecutor.getProcessingSize());
                requestCount.set(asyncExecutor.getLastHourRequests());
            });
            infoList.add(new ArrAsyncRequestVO(asyncExecutor.getType(), load.get(), requestCount.get(),
                    waiting.get(), running.get(), asyncExecutor.getWorkers(), workers.get()));
        });

        return infoList;
    }

    /**
     * Čekání na ukončení všech asynchronních požadavků - pro synchronizaci v unit testech.
     */
    public void waitForFinishAll() {
        asyncExecutors.values().forEach(AsyncExecutor::waitForFinish);
    }

    public void start() {
        boolean result = runningService.compareAndSet(false, true);
        if (result) {
            logger.info("Probíhá spouštění asynchronních front");
            initStart();
            logger.info("Dokončení spouštění asynchronních front");
        } else {
            logger.warn("Asynchronní fronty již běží");
        }
    }

    private void initStart() {
        asyncExecutors.values().forEach(AsyncExecutor::start);
    }

    public void stop() {
        boolean result;
        synchronized (this) {
            result = runningService.compareAndSet(true, false);
        }
        if (result) {
            logger.info("Zahájení zastavování asynchronních front");
            stopAll();
            logger.info("Zastaveny asynchronní fronty");
        } else {
            logger.warn("Asynchronní fronty jsou pozastavovány a nebo neběží");
        }
    }

    private void stopAll() {
        asyncExecutors.values().forEach(AsyncExecutor::stop);
    }

    /**
     * Event publish bulk action.
     *
     * @param bulkActionRun the bulk action run
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void eventPublishBulkAction(final ArrBulkActionRun bulkActionRun) {
        eventNotificationService.publishEvent(
                EventFactory.createIdInVersionEvent(
                        EventType.BULK_ACTION_STATE_CHANGE,
                        bulkActionRun.getFundVersion(),
                        bulkActionRun.getBulkActionRunId(),
                        bulkActionRun.getBulkActionCode(),
                        bulkActionRun.getState()
                )
        );
    }

    private static class AsyncBulkExecutor extends AsyncExecutor {

        private final BulkActionRunRepository bulkActionRepository;

        AsyncBulkExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund, final BulkActionRunRepository bulkActionRepository) {
            super(AsyncTypeEnum.BULK, executor, RequestQueue.of(new LinkedList<>()), txManager, asyncRequestRepository, appCtx, maxPerFund);
            this.bulkActionRepository = bulkActionRepository;
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncBulkActionWorker.class;
        }

        @Override
        protected boolean isFailedRequest(final ArrAsyncRequest request) {
            ArrBulkActionRun bulkAction = request.getBulkAction();
            ArrBulkActionRun.State state = bulkAction.getState();
            if (state == ArrBulkActionRun.State.RUNNING) {
                bulkAction.setState(ArrBulkActionRun.State.ERROR);
                bulkActionRepository.save(bulkAction);
                return true;
            } else if (state == ArrBulkActionRun.State.ERROR) {
                return true;
            }
            return false;
        }

    }

    private static class AsyncNodeExecutor extends AsyncExecutor {

        AsyncNodeExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund) {
            super(AsyncTypeEnum.NODE, executor, AsQueue.of(new PriorityQueue<>(1000, new NodeQueuePriorityComparator()), AsyncRequest::getNodeId, AsyncRequest::getFundVersionId, AsyncRequest::isFailed, new NodePriorityComparator()), txManager, asyncRequestRepository, appCtx, maxPerFund);
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncNodeWorker.class;
        }

        @Override
        protected boolean skip(final AsyncRequest request) {
            AsyncRequest existAsyncRequest = queue.findById(request.getNodeId());
            if (existAsyncRequest == null) {
                // neexistuje ve frontě, chceme přidat
                return false;
            } else {
                Integer priorityExists = existAsyncRequest.getPriority();
                Integer priorityAdding = request.getPriority();
                if (priorityAdding > priorityExists) {
                    // nově přidáváná položka má lepší prioritu; mažeme aktuální z fronty a vložíme novou
                    queue.remove(existAsyncRequest);
                    deleteRequest(existAsyncRequest);
                    return false;
                } else {
                    // nově přidáváná položka má horší prioritu, než je ve frontě; proto přeskakujeme
                    return true;
                }
            }
        }

        @Override
        public void onFail(IAsyncWorker worker, final Throwable error) {
            synchronized (lockQueue) {
                AsyncRequest request = worker.getRequest();
                logger.error("Selhání requestu {}", request, error);
                countRequest();
                processing.removeIf(next -> next.getRequest().getRequestId().equals(request.getRequestId()));
                if (worker.getRequests().size() > 1) {
                    for (AsyncRequest r : worker.getRequests()) {
                        r.setFailed(true);
                        enqueueInner(r);
                    }
                    resolveSkipped();
                } else {
                    deleteRequests(worker.getRequests());
                }
                scheduleNext();
            }
        }
    }

    private static class AsyncOutputExecutor extends AsyncExecutor {

        private final OutputRepository outputRepository;

        AsyncOutputExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund, final OutputRepository outputRepository) {
            super(AsyncTypeEnum.OUTPUT, executor, RequestQueue.of(new LinkedList<>()), txManager, asyncRequestRepository, appCtx, maxPerFund);
            this.outputRepository = outputRepository;
        }

        @Override
        protected boolean isFailedRequest(final ArrAsyncRequest request) {
            ArrOutput output = request.getOutput();
            ArrOutput.OutputState state = output.getState();
            if (state == ArrOutput.OutputState.GENERATING) {
                output.setState(ArrOutput.OutputState.OPEN);
                output.setError("Byl proveden restart serveru");
                outputRepository.save(output);
                return true;
            }
            return false;
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncOutputGeneratorWorker.class;
        }

    }

    private static class AsyncAccessPointExecutor extends AsyncExecutor {

        AsyncAccessPointExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx) {
            super(AsyncTypeEnum.AP, executor, RequestQueue.of(new LinkedList<>(), AsyncRequest::getAccessPointId), txManager, asyncRequestRepository, appCtx, Integer.MAX_VALUE);
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncAccessPointWorker.class;
        }

        @Override
        protected boolean skip(final AsyncRequest request) {
            AsyncRequest existAsyncRequest = queue.findById(request.getAccessPointId());
            if (existAsyncRequest == null) {
                // neexistuje ve frontě, chceme přidat
                return false;
            } else {
                Integer priorityExists = existAsyncRequest.getPriority();
                Integer priorityAdding = request.getPriority();
                if (priorityAdding > priorityExists) {
                    // nově přidáváná položka má lepší prioritu; mažeme aktuální z fronty a vložíme novou
                    queue.remove(existAsyncRequest);
                    deleteRequest(existAsyncRequest);
                    return false;
                } else {
                    // nově přidáváná položka má horší prioritu, než je ve frontě; proto přeskakujeme
                    return true;
                }
            }
        }
    }
}
