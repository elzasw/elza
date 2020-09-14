package cz.tacr.elza.service;

import cz.tacr.elza.asynchactions.AsyncNodeWorker;
import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.AsyncRequestEvent;
import cz.tacr.elza.asynchactions.AsyncWorkerVO;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.asynchactions.NodePriorityComparator;
import cz.tacr.elza.asynchactions.ThreadLoadInfo;
import cz.tacr.elza.asynchactions.TimeRequestInfo;
import cz.tacr.elza.bulkaction.AsyncAccessPointWorker;
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
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.tacr.elza.repository.ExceptionThrow.bulkAction;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * Servisní třída pro spouštění validací a hromadných akcí.
 */
@Service
@Configuration
@EnableAsync
public class AsyncRequestService implements ApplicationListener<AsyncRequestEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequestService.class);

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

    @Value("${elza.asyncActions.accessPoint.maxPerFund:1}")
    @Min(1)
    @Max(100)
    private int accessPointMaxPerFund;

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
    @Qualifier("threadPoolTaskExecutorOP")
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
        register(new AsyncAccessPointExecutor(accessPointTaskExecutor, txManager, asyncRequestRepository, appCtx, accessPointMaxPerFund));
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
        List<AsyncRequest> requests = new ArrayList<>(nodeList.size());
        for (ArrNode node : nodeList) {
            ArrAsyncRequest request = ArrAsyncRequest.create(fundVersion, node, priority == null ? 1 : priority);
            asyncRequestRepository.save(request);
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

        if (!originalState.equals(ArrBulkActionRun.State.WAITING) && !originalState.equals(ArrBulkActionRun.State.PLANNED) && !originalState.equals(ArrBulkActionRun.State.RUNNING)) {
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
            for (final AsyncRequest request : asyncExecutor.queue) {
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
                workers.set(convertWorkerList(asyncExecutor.processing));
                waiting.set(asyncExecutor.queue.size());
                running.set(asyncExecutor.processing.size());
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

    private static abstract class AsyncExecutor {

        /**
         * Čekací interval při aktivní čekání.
         */
        public final int WAIT = 10;

        /**
         * Časové okno pro výpočet zatížení.
         */
        public final int LOAD_SEC = 3600;

        private final ArrAsyncRequestRepository asyncRequestRepository;
        private final ApplicationContext appCtx;
        private final PlatformTransactionManager txManager;

        /**
         * Informace o zatížení.
         */
        private final ThreadLoadInfo threadLoadInfo = new ThreadLoadInfo(LOAD_SEC);

        /**
         * Informace o počtu požadavků za poslední hodinu.
         */
        private final List<TimeRequestInfo> lastHourRequests = new LinkedList<>();

        /**
         * Běží zpracovávání požadavků?
         */
        final AtomicBoolean running = new AtomicBoolean(false);

        /**
         * Správa vláken.
         */
        final ThreadPoolTaskExecutor executor;

        /**
         * Typ fronty.
         */
        final AsyncTypeEnum type;

        /**
         * Hlavní zámek pro přístup k datům.
         */
        final Object lockQueue = new Object();

        /**
         * Fronta čekajících požadavků.
         */
        final Queue<AsyncRequest> queue;

        /**
         * Seznam přeskočených požadavků na smazání.
         */
        final List<AsyncRequest> skipped = new ArrayList<>();

        /**
         * Seznam probíhajících zpracování.
         */
        final List<IAsyncWorker> processing = new ArrayList<>();

        /**
         * Maximální počet souběžných zpracování v rámci jedné verze archivního souboru.
         */
        final int maxPerFundVersion;

        AsyncExecutor(final AsyncTypeEnum type,
                      final ThreadPoolTaskExecutor executor,
                      final Queue<AsyncRequest> queue,
                      final PlatformTransactionManager txManager,
                      final ArrAsyncRequestRepository asyncRequestRepository,
                      final ApplicationContext appCtx,
                      final int maxPerFundVersion) {
            this.executor = executor;
            this.type = type;
            this.queue = queue;
            this.txManager = txManager;
            this.asyncRequestRepository = asyncRequestRepository;
            this.appCtx = appCtx;
            this.maxPerFundVersion = maxPerFundVersion;
        }

        /**
         * Pomocná metoda pro zjištění informací frontě na jedno zamčení.
         *
         * @param r spouštěný blok
         */
        public void doLockQueue(Runnable r) {
            synchronized (lockQueue) {
                r.run();
            }
        }

        /**
         * Jedná se o selhalý request?
         *
         * @param request request
         * @return je selhaný?
         */
        protected boolean isFailedRequest(final ArrAsyncRequest request) {
            return false;
        }

        /**
         * Zapsání časového okna pro vytížení.
         *
         * @param sec časové okno
         */
        public void writeSlot(int sec) {
            synchronized (lockQueue) {
                threadLoadInfo.getSlots()[sec] = processing.size();
            }
        }

        /**
         * Vypočtení aktuální zatížení.
         *
         * @return zatížení
         */
        public double getCurrentLoad() {
            synchronized (lockQueue) {
                double sum = 0;
                for (int s : threadLoadInfo.getSlots()) {
                    sum += s;
                }
                return sum / LOAD_SEC;
            }
        }

        /**
         * Počet požadavků za poslední hodinu.
         *
         * @return počet požadavků
         */
        public int getLastHourRequests() {
            synchronized (lockQueue) {
                lastHourRequests.removeIf(toDelete -> System.currentTimeMillis() - toDelete.getTimeFinished() > 3600000);
                return lastHourRequests.size();
            }
        }

        /**
         * Počet dostupných vláken.
         *
         * @return počet vláken
         */
        protected int getWorkers() {
            return executor.getCorePoolSize();
        }

        private void afterTx(final Runnable task) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        }

        /**
         * Spuštění úlohy v nové transakci.
         *
         * @param task úloha
         */
        private void tx(final Runnable task) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
            transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    task.run();
                }
            });
        }

        /**
         * Obnovení fronty z databáze.
         */
        private void restoreAndRun() {
            tx(() -> {
                logger.info("Obnovení databázové fronty {}", getType());
                List<AsyncRequest> results = new ArrayList<>();
                int p = 0;
                List<ArrAsyncRequest> requests;
                int MAX = 1000;
                List<ArrAsyncRequest> deleteRequests = new ArrayList<>();
                do {
                    requests = asyncRequestRepository.findRequestsByPriorityWithLimit(getType(), PageRequest.of(p, MAX));
                    for (ArrAsyncRequest request : requests) {
                        if (isFailedRequest(request)) {
                            deleteRequests.add(request);
                            logger.debug("Bude odstraněn požadavek z fronty z důvodu jeho chybového stavu. ID: {}", request.getAsyncRequestId());
                        } else {
                            results.add(new AsyncRequest(request));
                        }
                    }
                    p++;
                } while (requests.size() == MAX);

                if (deleteRequests.size() > 0) {
                    asyncRequestRepository.deleteAll(deleteRequests);
                }

                logger.info("Obnovení databázové fronty {} - obnoveno: {}, odebráno: {}", getType(), results.size(), deleteRequests.size());
                if (results.size() > 0) {
                    enqueue(results);
                }
            });
        }

        public AsyncTypeEnum getType() {
            return this.type;
        }

        protected void deleteRequest(AsyncRequest request) {
            deleteRequests(Collections.singletonList(request));
        }

        protected void deleteRequests(Collection<AsyncRequest> requests) {
            tx(() -> {
                for (AsyncRequest request : requests) {
                    logger.debug("Mazání requestu z DB: {}", request);
                    asyncRequestRepository.deleteByRequestId(request.getRequestId());
                }
            });
        }

        private void countRequest() {
            lastHourRequests.add(new TimeRequestInfo());
            lastHourRequests.removeIf(toDelete -> System.currentTimeMillis() - toDelete.getTimeFinished() > 3600000);
        }

        public void onFail(IAsyncWorker worker, final Throwable error) {
            synchronized (lockQueue) {
                AsyncRequest request = worker.getRequest();
                logger.error("Selhání requestu {}", request, error);
                countRequest();
                processing.removeIf(next -> next.getRequest().getRequestId().equals(request.getRequestId()));
                deleteRequest(worker.getRequest());
                scheduleNext();
            }
        }

        public void onSuccess(IAsyncWorker worker) {
            synchronized (lockQueue) {
                AsyncRequest request = worker.getRequest();
                logger.debug("Dokončení requestu {}", request);
                countRequest();
                processing.removeIf(next -> next.getRequest().getRequestId().equals(request.getRequestId()));
                deleteRequest(worker.getRequest());
                scheduleNext();
            }
        }

        public void terminate(Integer currentId) {
            List<IAsyncWorker> terminateWorkers = new ArrayList<>();
            synchronized (lockQueue) {
                Iterator<AsyncRequest> iterator = queue.iterator();
                List<AsyncRequest> removed = new ArrayList<>();
                while (iterator.hasNext()) {
                    AsyncRequest request = iterator.next();
                    if (currentId.equals(request.getCurrentId())) {
                        iterator.remove();
                        removed.add(request);
                    }
                }
                if (removed.size() > 0) {
                    deleteRequests(removed);
                }
                for (IAsyncWorker worker : processing) {
                    AsyncRequest request = worker.getRequest();
                    if (currentId.equals(request.getCurrentId())) {
                        terminateWorkers.add(worker);
                    }
                }
            }
            for (IAsyncWorker worker : terminateWorkers) {
                AsyncRequest request = worker.getRequest();
                logger.debug("Ukončuji {} request: {}", getType(), request.getRequestId());
                worker.terminate();
            }
        }

        public void terminateFund(Integer fundVersionId) {
            List<IAsyncWorker> terminateWorkers = new ArrayList<>();
            synchronized (lockQueue) {
                Iterator<AsyncRequest> iterator = queue.iterator();
                List<AsyncRequest> removed = new ArrayList<>();
                while (iterator.hasNext()) {
                    AsyncRequest next = iterator.next();
                    if (fundVersionId.equals(next.getFundVersionId())) {
                        iterator.remove();
                        removed.add(next);
                    }
                }
                if (removed.size() > 0) {
                    deleteRequests(removed);
                }
                for (IAsyncWorker worker : processing) {
                    AsyncRequest request = worker.getRequest();
                    if (fundVersionId.equals(request.getFundVersionId())) {
                        terminateWorkers.add(worker);
                    }
                }
            }
            for (IAsyncWorker worker : terminateWorkers) {
                AsyncRequest request = worker.getRequest();
                logger.debug("Ukončuji {} request: {}", getType(), request.getRequestId());
                worker.terminate();
            }
        }

        protected boolean isEmptyWorker() {
            return processing.size() < getWorkers();
        }

        @Nullable
        protected AsyncRequest selectNext() {
            Map<Integer, Integer> fundVersionCount = calcFundVersionsPerWorkers();

            AsyncRequest next;
            AsyncRequest selected = null;
            List<AsyncRequest> backToQueue = new ArrayList<>();
            while ((next = queue.poll()) != null) {
                Integer fundVersionId = next.getFundVersionId();
                Integer count = fundVersionCount.getOrDefault(fundVersionId, 0);
                if (count < maxPerFundVersion) {
                    selected = next;
                    break;
                } else {
                    backToQueue.add(next);
                }
            }

            // vracení požadavků do fronty
            if (backToQueue.size() > 0) {
                queue.addAll(backToQueue);
            }

            return selected;
        }

        protected Map<Integer, Integer> calcFundVersionsPerWorkers() {
            Map<Integer, Integer> fundVersionCount = new HashMap<>();
            for (IAsyncWorker asyncWorker : processing) {
                AsyncRequest request = asyncWorker.getRequest();
                Integer fundVersionId = request.getFundVersionId();
                Integer count = fundVersionCount.get(fundVersionId);
                if (count == null) {
                    count = 0;
                }
                count++;
                fundVersionCount.put(fundVersionId, count);
            }
            return fundVersionCount;
        }

        private void scheduleNext() {
            while (!queue.isEmpty() && isEmptyWorker() && running.get()) {
                AsyncRequest request = selectNext();
                if (request != null) {
                    IAsyncWorker worker = appCtx.getBean(workerClass(), request);
                    logger.debug("Naplánování requestu: {}", request);
                    processing.add(worker);
                    executor.submit(worker);
                } else {
                    logger.debug("Nebyl vybrán žádný request {}", getType());
                    break;
                }
            }
        }

        /**
         * Třída pro zpracování požadavku.
         */
        protected abstract Class<? extends IAsyncWorker> workerClass();

        /**
         * Vložení do fronty.
         *
         * @param request požadavek na zpracování
         */
        public void enqueue(final AsyncRequest request) {
            enqueue(Collections.singleton(request));
        }

        protected void enqueueInner(final AsyncRequest request) {
            if (request.getType() != getType()) {
                throw new IllegalStateException("Neplatný typ požadavku");
            }
            // detekci, zda-li se má požadavek přidat nebo přeskočit
            if (skip(request)) {
                skipped.add(request); // přidání do fronty na přeskočení
            } else {
                logger.debug("Přidání do fronty: {}", request);
                queue.add(request);
            }
        }

        /**
         * Přeskočit požadavek?
         *
         * @param request požadavek na zpracování
         * @return true - ano, přeskočit
         */
        protected boolean skip(final AsyncRequest request) {
            return false;
        }

        /**
         * Synchronní čekání na dokončení všech požadavků.
         */
        void waitForFinish() {
            boolean finish;
            int waiting = 0;
            do {
                synchronized (lockQueue) {
                    finish = processing.isEmpty() && queue.isEmpty();
                }
                if (!finish) {
                    try {
                        if (waiting % 1000 == 0) {
                            logger.info("Čekání na ukončení asynchronní fronty: {}", getType());
                        }
                        Thread.sleep(WAIT);
                        waiting += WAIT;
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Přerušení čekajícího vlákna", e);
                    }
                }
            } while (!finish);
        }

        /**
         * Hromadné přidání požadavků.
         *
         * @param requests požadavky na zpracování
         */
        public void enqueue(final Collection<AsyncRequest> requests) {
            // přidáváme až po úspěšném dokončení probíhající transakce
            afterTx(() -> {
                synchronized (lockQueue) {
                    for (AsyncRequest asyncRequest : requests) {
                        enqueueInner(asyncRequest);
                    }
                    resolveSkipped();
                    scheduleNext();
                }
            });
        }

        /**
         * Vyřešení přeskočených požadavků - je třeba je smazat z DB.
         */
        private void resolveSkipped() {
            if (skipped.size() > 0) {
                logger.debug("Přeskočeny požadavky z fronty {}: {}, {}", getType(), skipped.size(), skipped.stream()
                        .map(AsyncRequest::getRequestId)
                        .collect(Collectors.toList()));
                deleteRequests(skipped);
                skipped.clear();
            }
        }

        /**
         * Zastavení zpracování.
         */
        public void stop() {
            boolean result = running.compareAndSet(true, false);
            if (result) {
                synchronized (lockQueue) {
                    queue.clear();
                }
                int i;
                do {
                    synchronized (lockQueue) {
                        i = processing.size();
                    }
                    if (i > 0) {
                        try {
                            Thread.sleep(WAIT);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException("Přerušení uspání vlákna", e);
                        }
                    }
                } while (i > 0);
            }
        }

        /**
         * Spuštění zpracování.
         */
        void start() {
            synchronized (lockQueue) {
                boolean result = running.compareAndSet(false, true);
                if (result) {
                    restoreAndRun();
                }
            }
        }

        /**
         * Zpracovává se (nebo je ve frontě) něco z verze archivní pomůcky.
         *
         * @param fundVersionId id verze archivní pomůcky
         * @return true - je zpracováno něco z verze archivní pomůcky
         */
        public boolean isProcessing(final Integer fundVersionId) {
            synchronized (lockQueue) {
                for (AsyncRequest request : queue) {
                    if (fundVersionId.equals(request.getFundVersionId())) {
                        return true;
                    }
                }
                for (IAsyncWorker worker : processing) {
                    AsyncRequest request = worker.getRequest();
                    if (fundVersionId.equals(request.getFundVersionId())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private static class AsyncBulkExecutor extends AsyncExecutor {

        private final BulkActionRunRepository bulkActionRepository;

        AsyncBulkExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund, final BulkActionRunRepository bulkActionRepository) {
            super(AsyncTypeEnum.BULK, executor, new LinkedList<>(), txManager, asyncRequestRepository, appCtx, maxPerFund);
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
            super(AsyncTypeEnum.NODE, executor, new PriorityQueue<>(1000, new NodePriorityComparator()), txManager, asyncRequestRepository, appCtx, maxPerFund);
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncNodeWorker.class;
        }

        @Override
        protected boolean skip(final AsyncRequest request) {
            Map<Integer, AsyncRequest> queuedNodeIds = queue.stream()
                    .collect(Collectors.toMap(AsyncRequest::getNodeId, Function.identity()));
            AsyncRequest existAsyncRequest = queuedNodeIds.get(request.getNodeId());
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

    private static class AsyncOutputExecutor extends AsyncExecutor {

        private final OutputRepository outputRepository;

        AsyncOutputExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund, final OutputRepository outputRepository) {
            super(AsyncTypeEnum.OUTPUT, executor, new LinkedList<>(), txManager, asyncRequestRepository, appCtx, maxPerFund);
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

        AsyncAccessPointExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager, final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx, final int maxPerFund) {
            super(AsyncTypeEnum.AP, executor, new LinkedList<>(), txManager, asyncRequestRepository, appCtx, maxPerFund);
        }

        @Override
        protected Class<? extends IAsyncWorker> workerClass() {
            return AsyncAccessPointWorker.class;
        }

        @Override
        protected boolean skip(final AsyncRequest request) {
            Map<Integer, AsyncRequest> queuedNodeIds = queue.stream()
                    .collect(Collectors.toMap(AsyncRequest::getAccessPointId, Function.identity()));
            AsyncRequest existAsyncRequest = queuedNodeIds.get(request.getAccessPointId());
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
