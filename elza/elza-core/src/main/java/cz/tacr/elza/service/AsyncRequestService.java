package cz.tacr.elza.service;

import cz.tacr.elza.asynchactions.*;
import cz.tacr.elza.bulkaction.AsyncBulkActionWorker;
import cz.tacr.elza.bulkaction.BulkActionHelperService;
import cz.tacr.elza.controller.vo.ArrAsyncRequestVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.FundStatisticsVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;
import cz.tacr.elza.repository.BulkActionRunRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.service.output.AsyncOutputGeneratorWorker;
import org.apache.commons.lang3.NotImplementedException;
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
import org.springframework.transaction.support.*;

import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servisní třída pro spouštění validací a hromadných akcí
 */
@Service
@Configuration
@EnableAsync
public class AsyncRequestService implements ApplicationListener<AsyncRequestEvent> {

    private final Map<Integer, List<IAsyncWorker>> runningNodeWorkers = new ConcurrentHashMap<>(); //fundVersionId, List

    private final Map<Integer, IAsyncWorker> runningBulkActionWorkers = new ConcurrentHashMap<>(); //fundVersionId, List

    private final Set<IAsyncWorker> runningOutputGeneratorWorkers = new HashSet<>();

    private Map<Integer, PriorityQueue<AsyncRequestVO>> nodeMap; //fundVersionId, Queue

    private Map<Integer, Queue<AsyncRequestVO>> bulkActionMap; //fundVersionId, Queue

    private Map<Integer, Long> runningNodeIdMap; //nodeId, requestId

    private Map<Integer, Long> runningBulkActionMap; //fundVersionId, requestId

    private Map<Integer, Long> waitingNodeRequestMap; //nodeId, requestId

    private Queue<AsyncRequestVO> outputQueue;

    private ThreadLoadInfo threadLoadInfo;

    private List<FundStatisticsVO> nodeFundStatistics;

    private List<FundStatisticsVO> bulkFundStatistics;

    private List<FundStatisticsVO> outputFundStatistics;

    private List<TimeRequestInfo> lastHourRequests;

    @Autowired
    private ApplicationContext appCtx;

    @Autowired
    private ArrAsyncRequestRepository asyncRequestRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private BulkActionRunRepository bulkActionRepository;

    @Autowired
    private BulkActionHelperService bulkActionHelperService;

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
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    @Value("${elza.asyncActions.node.requestCount:}")
    @Min(1)
    @Max(100)
    private int MAX_PROCESSED_NODES;

    @Value("${elza.asyncActions.bulk.requestCount:}")
    @Min(1)
    @Max(100)
    private int MAX_PROCESSED_BULK_ACTIONS;

    @Value("${elza.asyncActions.output.requestCount:}")
    @Min(1)
    @Max(100)
    private int MAX_PROCESSED_OUTPUTS;

    private Integer terminatedNodeFundId;

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequestService.class);

    public AsyncRequestService() {
        lastHourRequests = new LinkedList<>();
        threadLoadInfo = new ThreadLoadInfo();
        nodeMap = new ConcurrentHashMap<>();
        bulkActionMap = new ConcurrentHashMap<>();
        outputQueue = new LinkedList<>();

        runningNodeIdMap = new ConcurrentHashMap<>();
        waitingNodeRequestMap = new ConcurrentHashMap<>();
        runningBulkActionMap = new ConcurrentHashMap<>();
    }

    /**
     * Přídání výstupu do fronty na zpracování
     * @param fundVersion
     * @param output
     * @param type
     * @param validationPriority
     */
    public void enqueue(ArrFundVersion fundVersion, ArrOutput output, AsyncTypeEnum type, Integer validationPriority) {
        if (type != AsyncTypeEnum.OUTPUT) {
            throw new IllegalArgumentException("Nesprávný typ asynchronní akce pro hromadnou akci: " + type);
        }
        if (validationPriority == null) {
            validationPriority = 1;
        }
        ArrAsyncRequest request = new ArrAsyncRequest(type, validationPriority, fundVersion);
        request.setOutput(output);
        asyncRequestRepository.save(request);
        AsyncRequestVO mapRequest = new AsyncRequestVO(request);
        addToMap(mapRequest);
        beginValidation(type);
    }

    /**
     * Přidání hromadné akce do fronty na zpracování
     * @param fundVersion
     * @param bulkActionRun
     * @param type
     * @param validationPriority
     */
    public void enqueue(ArrFundVersion fundVersion, ArrBulkActionRun bulkActionRun, AsyncTypeEnum type, Integer validationPriority) {
        if (type != AsyncTypeEnum.BULK) {
            throw new IllegalArgumentException("Nesprávný typ asynchronní akce pro hromadnou akci: " + type);
        }
        if (validationPriority == null) {
            validationPriority = 1;
        }
        bulkActionHelperService.eventPublishBulkAction(bulkActionRun);
        ArrAsyncRequest request = new ArrAsyncRequest(type, validationPriority, fundVersion);
        request.setBulkAction(bulkActionRun);
        asyncRequestRepository.save(request);
        AsyncRequestVO mapRequest = new AsyncRequestVO(request);
        addToMap(mapRequest);
        beginValidation(type);
    }

    /**
     * Přidání Nodů do fronty ke zpracování
     * @param fundVersion
     * @param nodeList
     * @param type
     * @param validationPriority
     */

    public void enqueue(ArrFundVersion fundVersion, List<ArrNode> nodeList, AsyncTypeEnum type, Integer validationPriority) {
        if (type != AsyncTypeEnum.NODE) {
            throw new IllegalArgumentException("Nesprávný typ asynchronní akce pro ověření nodů: " + type);
        }
        if (validationPriority == null) {
            validationPriority = 1;
        }
        for (ArrNode node : nodeList) {
            ArrAsyncRequest request = new ArrAsyncRequest(type, validationPriority, fundVersion);
            request.setNode(node);
            asyncRequestRepository.save(request);
            AsyncRequestVO mapRequest = new AsyncRequestVO(request);
            addToMap(mapRequest);
        }
        beginValidation(type);
    }

    /**
     * Začátek validace po dokončení předchozí transakce
     * @param type
     */
    private void beginValidation(AsyncTypeEnum type) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                logger.debug("Begin validation : " + type);
                selecteNextWorker(type);
            }
        });
    }

    /**
     * Kontrola, jestli běží validace AS před mazáním celého AS
      * @param version
     * @return
     */
    public synchronized boolean isFundNodeRunning(final ArrFundVersion version) {
        if (runningNodeWorkers.containsKey(version.getFundVersionId())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Kontrola, jestli běží hromadná akce na AS před mazáním celého AS
     * @param version
     * @return
     */
    public synchronized boolean isFundBulkActionRunning(final ArrFundVersion version) {
        if (runningBulkActionWorkers.containsKey(version.getFundVersionId())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Mazání požadavku z DB fronty po vykonání úkolu
     * @param requestId
     */
    public synchronized void deleteRequestFromRepository(Long requestId) {
        asyncRequestRepository.deleteByRequestId(requestId);
    }

    /**
     * Načtení dat z DB při prázdné frontě požadavků ke zpracování, rozděleno podle typu požadavků
     * @param type
     */
    private void findWorkerData(AsyncTypeEnum type) {
        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                switch (type) {
                    case NODE:
                        synchronized (this) {
                            if (nodeMap == null || nodeMap.isEmpty()) {
                                if (nodeFundStatistics == null) {
                                    nodeFundStatistics = new ArrayList<>();
                                }
                                List<ArrAsyncRequest> queuedRequests;
                                if (runningNodeWorkers.isEmpty()) {
                                    queuedRequests = asyncRequestRepository.findNodeRequestsByPriorityWithLimit(type, new PageRequest(0, MAX_PROCESSED_NODES));
                                } else {
                                    queuedRequests = asyncRequestRepository.findNodeRequestsByPriorityAndRequestsWithLimit(type, getCurrentWorkersNodeIds(), new PageRequest(0, MAX_PROCESSED_NODES));
                                }
                                for (ArrAsyncRequest request : queuedRequests) {
                                    addToMap(new AsyncRequestVO(request));
                                }
                            }
                        }
                        break;
                    case BULK:
                        synchronized (this) {
                            if (bulkActionMap == null || bulkActionMap.isEmpty()) {
                                if (bulkActionMap == null) {
                                    bulkActionMap = new HashMap<>();
                                }
                                if (bulkFundStatistics == null) {
                                    bulkFundStatistics = new ArrayList<>();
                                }
                                List<ArrAsyncRequest> queuedRequests = asyncRequestRepository.findRequestsByPriorityWithLimit(type, new PageRequest(0, MAX_PROCESSED_BULK_ACTIONS));
                                for (ArrAsyncRequest request : queuedRequests) {
                                    addToMap(new AsyncRequestVO(request));
                                }
                            }
                        }
                        break;
                    case OUTPUT:
                        synchronized (this) {
                            if (outputQueue == null || outputQueue.isEmpty()) {
                                if (outputQueue == null) {
                                    outputQueue = new LinkedList<>();
                                }
                                if (outputFundStatistics == null) {
                                    outputFundStatistics = new ArrayList<>();
                                }
                                List<ArrAsyncRequest> queuedRequests = asyncRequestRepository.findRequestsByPriorityWithLimit(type, new PageRequest(0, MAX_PROCESSED_OUTPUTS));
                                for (ArrAsyncRequest request : queuedRequests) {
                                    outputQueue.add(new AsyncRequestVO(request));
                                }
                            }
                        }
                        break;
                }
            }
        });
    }

    /**
     * Přidání požadavku do seznamu pro zpracování
     * @param request
     */
    private synchronized void addToMap(AsyncRequestVO request) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                Integer fundVersionId = request.getFundVersionId();
                switch (request.getType()) {
                    case NODE:
                        if (nodeMap == null) {
                            nodeMap = new ConcurrentHashMap<>();
                        }
                        if (nodeFundStatistics == null) {
                            nodeFundStatistics = new ArrayList<>();
                        }
                        if (nodeMap.containsKey(fundVersionId)) {
                            PriorityQueue<AsyncRequestVO> requests = nodeMap.get(fundVersionId);
                            requests.add(request);
                            nodeMap.put(fundVersionId, requests);
                            addToFundStatistics(AsyncTypeEnum.NODE, fundVersionId, 1);
                        } else {
                            PriorityQueue<AsyncRequestVO> requests = new PriorityQueue<>(1000, new NodePriorityComparator());
                            requests.add(request);
                            nodeMap.put(fundVersionId, requests);
                            addToFundStatistics(AsyncTypeEnum.NODE, fundVersionId, 1);
                        }
                        waitingNodeRequestMap.put(request.getNodeId(), request.getRequestId());
                        break;
                    case BULK:
                        if (bulkActionMap == null) {
                            bulkActionMap = new HashMap<>();
                        }
                        if (bulkFundStatistics == null) {
                            bulkFundStatistics = new ArrayList<>();
                        }
                        if (bulkActionMap.containsKey(fundVersionId)) {
                            Queue<AsyncRequestVO> requests = bulkActionMap.get(fundVersionId);
                            requests.add(request);
                            bulkActionMap.put(fundVersionId, requests);
                            addToFundStatistics(AsyncTypeEnum.BULK, fundVersionId, 1);
                        } else {
                            Queue<AsyncRequestVO> requests = new LinkedList<>();
                            requests.add(request);
                            bulkActionMap.put(fundVersionId, requests);
                            addToFundStatistics(AsyncTypeEnum.BULK, fundVersionId, 1);
                        }
                        break;
                    case OUTPUT:
                        if (outputQueue == null) {
                            outputQueue = new LinkedList<>();
                        }
                        if (outputFundStatistics == null) {
                            outputFundStatistics = new ArrayList<>();
                        }
                        outputQueue.add(request);
                        addToFundStatistics(AsyncTypeEnum.OUTPUT, fundVersionId, 1);
                        break;
                }
            }
        });
    }

    /**
     * Výběr dalšího workeru, po začátku validace, nebo po skončení předchozího workeru
     * @param type
     */
    private void selecteNextWorker(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                prepareNextNodeWorker(type);
                break;
            case BULK:
                prepareNextBulkActionWorker(type);
                break;
            case OUTPUT:
                prepareNextOutputWorker(type);
                break;
        }
    }

    /**
     * Příprava workeru pro zpracování NODů
     * @param type
     */
    private synchronized void prepareNextNodeWorker(AsyncTypeEnum type) {
        if (nodeMap == null || nodeMap.isEmpty()) {
            findWorkerData(type);
        }
        // Načtení všech nodů podle AS
        Iterator<Map.Entry<Integer, PriorityQueue<AsyncRequestVO>>> nodeIterator = nodeMap.entrySet().iterator();
        List<Integer> toDelete = new ArrayList<>();
        // Procházezní jednotlivých AS s kontrolou, jestli je možn=é spustit nový worker a jestli ve frontě existují požadavky
        while (canRunNewWorker(type) && nodeIterator.hasNext()) {
            Map.Entry<Integer, PriorityQueue<AsyncRequestVO>> fundAsyncRequests = nodeIterator.next();
            Integer currentFundId = fundAsyncRequests.getKey();
            PriorityQueue<AsyncRequestVO> asyncRequestsQueue = fundAsyncRequests.getValue();
            /**
             * Volání spuštění workeru nad NODem, v případě, že :
             * - je možné spustit worker - je volné vlákno
             * - vybraný NOD není aktuálně zpracovávaný jiným workerem pod jiným requestem
             * - jestli už pod aktuálním AS něběží jiným worker, pokud jsou ve frontě AS jiné AS s požadavky ke zpracování
             */
            while (canRunNewWorker(type) && canRunNode(asyncRequestsQueue, currentFundId) && !(runningNodeWorkers.containsKey(currentFundId) && nodeIterator.hasNext())) {
                runNextNodeWorker(asyncRequestsQueue.poll(), currentFundId);
            }
            if (asyncRequestsQueue.isEmpty()) {
                toDelete.add(currentFundId);
            }
        }
        // vymazání AS z interní fronty požadavků, pokud AS nemá žádný přiřazený požadavek
        for (Integer index : toDelete) {
            nodeMap.remove(index);
        }
    }

    /**
     * Kontrola, jestli není vybraný NOD zpracovávaný v jiném workeru
     * @param asyncRequestsQueue
     * @param fundVersionId
     * @return
     */
    private boolean canRunNode(PriorityQueue<AsyncRequestVO> asyncRequestsQueue, Integer fundVersionId) {
        Integer nodeId;
        if (asyncRequestsQueue.isEmpty()) {
            return false;
        } else {
            nodeId = asyncRequestsQueue.peek().getNodeId();
        }
        if (runningNodeIdMap.containsKey(nodeId)) {
            return false;
        }
        return true;
    }

    /**
     * Seznam aktuálně zpracovávaných NODů
     * @return
     */
    private List<Long> getCurrentWorkersNodeIds() {
        List<Long> workerNodeIds = new ArrayList<>();
        for (Map.Entry<Integer, List<IAsyncWorker>> entry : runningNodeWorkers.entrySet()) {
            for (IAsyncWorker worker : entry.getValue()) {
                workerNodeIds.add(worker.getRequestId());
            }
        }
        return workerNodeIds;
    }

    /**
     * Příprava hromadné akce ke zpracování
     * @param type
     */
    private synchronized void prepareNextBulkActionWorker(AsyncTypeEnum type) {
        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                if (bulkActionMap == null || bulkActionMap.isEmpty()) {
                    findWorkerData(type);
                }
                Iterator<Map.Entry<Integer, Queue<AsyncRequestVO>>> bulkActionIterator = bulkActionMap.entrySet().iterator();
                // Procházezní jednotlivých AS s kontrolou, jestli je možné spustit nový worker a jestli ve frontě existují požadavky
                while (canRunNewWorker(type) && bulkActionIterator.hasNext()) {
                    Map.Entry<Integer, Queue<AsyncRequestVO>> fundAsyncRequests = bulkActionIterator.next();
                    Integer currentFundId = fundAsyncRequests.getKey();
                    Queue<AsyncRequestVO> asyncRequestsQueue = fundAsyncRequests.getValue();
                    // Kontrola, jestli je možné spustit worker a jestli nad daným AS neběží jiná hromadná akce a jestli existuje hromadná akce ke zpracování
                    while (canRunNewWorker(type) && canBulkActionRun(currentFundId) && !asyncRequestsQueue.isEmpty()) {
                        runningBulkActionMap.put(currentFundId, asyncRequestsQueue.peek().getRequestId());
                        runBulkAction(asyncRequestsQueue.poll(), currentFundId);
                    }
                    if (asyncRequestsQueue.isEmpty()) {
                        bulkActionIterator.remove();
                    }
                }
            }
        });
    }

    /**
     * Příprava výstupu ke zpracování
     * @param type
     */
    private synchronized void prepareNextOutputWorker(AsyncTypeEnum type) {
        if (outputQueue == null || outputQueue.isEmpty()) {
            findWorkerData(type);
        }
        while (canRunNewWorker(type) && !outputQueue.isEmpty()) {
            AsyncRequestVO request = outputQueue.poll();
            IAsyncWorker worker = appCtx.getBean(AsyncOutputGeneratorWorker.class, request.getFundVersionId(), request.getOutputId(), request.getRequestId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        runningOutputGeneratorWorkers.add(worker);
                        outputTaskExecutor.execute(worker);
                    } else {
                        logger.warn("Request for output is cancelled due to rollback of source transaction, fundVersionId:{}",
                                worker.getFundVersionId());
                    }
                }
            });
        }
    }

    /**
     * Spuštění workeru pro zpracování NODu
     * @param request
     * @param currentFundId
     */
    private void runNextNodeWorker(AsyncRequestVO request, Integer currentFundId) {
        runningNodeIdMap.put(request.getNodeId(), request.getRequestId());
        waitingNodeRequestMap.remove(request.getNodeId());
        IAsyncWorker worker = appCtx.getBean(AsyncNodeWorker.class, currentFundId, request.getRequestId(), request.getNodeId());
        if (runningNodeWorkers.containsKey(worker.getFundVersionId())) {
            runningNodeWorkers.get(worker.getFundVersionId()).add(worker);
        } else {
            List<IAsyncWorker> workers = new ArrayList<>();
            workers.add(worker);
            runningNodeWorkers.put(worker.getFundVersionId(), workers);
        }
        nodeTaskExecutor.execute(worker);
    }

    /**
     * Přidání požadavku do statistiky požadavků pro jednotlivé AS
     * @param type
     * @param fundVersionId
     * @param requestCount
     */
    private void addToFundStatistics(AsyncTypeEnum type, int fundVersionId, int requestCount) {
        switch (type) {
            case NODE:
                boolean nodeFundStatFound = false;
                for (FundStatisticsVO stat : nodeFundStatistics) {
                    if (stat.getFundVersionId() == fundVersionId) {
                        int count = stat.getRequestCount();
                        count += requestCount;
                        stat.setRequestCount(count);
                        nodeFundStatFound = true;
                        break;
                    }
                }
                if (!nodeFundStatFound) {
                    nodeFundStatistics.add(createFundStatisticsVO(fundVersionId));
                }
                break;
            case BULK:
                boolean bulkFundStatFound = false;
                for (FundStatisticsVO stat : bulkFundStatistics) {
                    if (stat.getFundVersionId() == fundVersionId) {
                        int count = stat.getRequestCount();
                        count += requestCount;
                        stat.setRequestCount(count);
                        bulkFundStatFound = true;
                        break;
                    }
                }
                if (!bulkFundStatFound) {
                    bulkFundStatistics.add(createFundStatisticsVO(fundVersionId));
                }
                break;
            case OUTPUT:
                boolean outputFundStatFound = false;
                for (FundStatisticsVO stat : outputFundStatistics) {
                    if (stat.getFundVersionId() == fundVersionId) {
                        int count = stat.getRequestCount();
                        count += requestCount;
                        stat.setRequestCount(count);
                        outputFundStatFound = true;
                        break;
                    }
                }
                if (!outputFundStatFound) {
                    outputFundStatistics.add(createFundStatisticsVO(fundVersionId));
                }
                break;
            default:
                throw new NotImplementedException("Typ requestu pro smazání neni implementován: " + type);
        }
    }

    /**
     * Vytvoření statistické třídy pro AS, pokud neexistuje
     * @param fundVersionId
     * @return
     */
    private FundStatisticsVO createFundStatisticsVO(int fundVersionId) {
        FundStatisticsVO stat = new FundStatisticsVO(fundVersionId);
        ArrFundVersion version = fundVersionRepository.findByIdWithFetchFund(fundVersionId);
        ArrFundVO statFund = new ArrFundVO();
        statFund.setName(version.getFund().getName());
        statFund.setId(version.getFund().getFundId());
        statFund.setInstitutionId(version.getFund().getInstitution().getInstitutionId());
        stat.setFund(statFund);
        stat.setRequestCount(1);
        return stat;
    }

    /**
     * Přidání informace o vzniku požadavku do statistiky o zpracování požadavků za poslední hodinu
     * @param info
     */
    public synchronized void addToLastHourRequests(TimeRequestInfo info) {
        lastHourRequests.add(info);
        for (Iterator<TimeRequestInfo> iterator = lastHourRequests.iterator(); iterator.hasNext(); ) {
            TimeRequestInfo toDelete = iterator.next();
            if (System.currentTimeMillis() - toDelete.getTimeFinished() > 3600000) {
                iterator.remove();
            }
        }
    }

    /**
     * získání počtu běžících workerů pro NODy
     * @return
     */
    private int getRunningNodeWorkersCount() {
        int runningNodeWorkersCount = 0;
        for (Map.Entry<Integer, List<IAsyncWorker>> entry : runningNodeWorkers.entrySet()) {
            runningNodeWorkersCount += entry.getValue().size();
        }
        return runningNodeWorkersCount;
    }

    /**
     * kontrola, jestli existuje volné vlákno pro zpracování požadavku
     * @param type
     * @return
     */
    private synchronized boolean canRunNewWorker(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                return (getRunningNodeWorkersCount() < nodeTaskExecutor.getCorePoolSize());
            case BULK:
                return (runningBulkActionWorkers.size() < bulkActionTaskExecutor.getCorePoolSize());
            case OUTPUT:
                return (runningOutputGeneratorWorkers.size() < outputTaskExecutor.getCorePoolSize());
            default:
                throw new NotImplementedException("Typ requestu pro smazání neni implementován: " + type);
        }
    }

    /**
     * Testuje, zda-li může být úloha spuštěna/naplánována.
     *
     * @param fundVersionId ID archivn9ho soubor, podle které se testuje možné spuštění
     * @return true - pokud se může spustit
     */
    private synchronized boolean canBulkActionRun(final Integer fundVersionId) {
        return !runningBulkActionMap.containsKey(fundVersionId);
    }

    /**
     * spuštění hromadné akce
     * @param requestVO
     * @param fundVersionId
     */
    private void runBulkAction(AsyncRequestVO requestVO, Integer fundVersionId) {
        AsyncBulkActionWorker bulkActionWorker = this.appCtx.getBean(AsyncBulkActionWorker.class, fundVersionId, requestVO.getRequestId(), requestVO.getBulkActionId());
        bulkActionTaskExecutor.execute(bulkActionWorker);
    }

    /**
     * Kontrola, jestli je možné validovat
     * @param asyncRequestId
     * @param nodeId
     * @return
     */
    public synchronized boolean canUpdateConformity(Long asyncRequestId, Integer nodeId) {
        if (waitingNodeRequestMap.get(nodeId) != null) {
            if (waitingNodeRequestMap.get(nodeId) > asyncRequestId) {
                return false;
            }
        }
        if (runningNodeIdMap.containsKey(nodeId)) {
            if (runningNodeIdMap.get(nodeId) == asyncRequestId) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * odebrání hromadné akce ze seznamu pro zpracování při jejím přerušení
     * @param type
     * @param id
     */
    public void removeFromArrAsyncRequest(AsyncTypeEnum type, Integer id) {
        switch (type) {
            case BULK:
                asyncRequestRepository.deleteByBulkActionRunId(id);
                break;
            default:
                throw new NotImplementedException("Typ requestu pro smazání neni implementován: " + type);
        }
    }

    /**
     * Odebrání ze seznamu bežících workerů pro NODy po jeho ukončení
     * @param fundVersionId
     * @param requestId
     */
    private void removeFromRunningNodeWorkers(Integer fundVersionId, Long requestId) {
        List<IAsyncWorker> workers = runningNodeWorkers.get(fundVersionId);
        if (workers != null) {
            for (IAsyncWorker worker : workers) {
                if (worker.getRequestId() == requestId) {
                    workers.remove(worker);
                    if (workers.isEmpty()) {
                        runningNodeWorkers.remove(fundVersionId);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Přeruší všechny akce pro danou verzi. (všechny naplánované + čekající)
     * <p>
     * Synchronní metoda, čeká na přerušení
     *
     * @param fundVersionId id verze archivní pomůcky
     */
    public void terminateBulkActions(final Integer fundVersionId) {
        // TODO: Toto řešení nedává smysl pro mazání AS odkud je funkce volána
        //       Bude nutné více přepracovat, např. rovnou hromadné akce smazat.
        bulkActionRepository.findByFundVersionIdAndState(fundVersionId, ArrBulkActionRun.State.WAITING).forEach(bulkActionRun -> {
            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);
            bulkActionRepository.save(bulkActionRun);
        });
        bulkActionRepository.flush();

        if (runningBulkActionWorkers.containsKey(fundVersionId)) {
            runningBulkActionWorkers.get(fundVersionId).terminate();
        }
    }

    /**
     * Přeruší hromadnou akci pokud je ve stavu - čeká | plánování | běh
     *
     * @param bulkActionId Id hromadné akce
     */
    public void interruptBulkAction(final int bulkActionId) {
        ArrBulkActionRun bulkActionRun = bulkActionRepository.findOne(bulkActionId);

        if (bulkActionRun == null) {
            throw new IllegalArgumentException("Hromadná akce s ID " + bulkActionId + " nebyla nalezena!");
        }
        ArrBulkActionRun.State originalState = bulkActionRun.getState();

        if (!originalState.equals(ArrBulkActionRun.State.WAITING) && !originalState.equals(ArrBulkActionRun.State.PLANNED) && !originalState.equals(ArrBulkActionRun.State.RUNNING)) {
            throw new IllegalArgumentException("Nelze přerušit hromadnou akci ve stavu " + originalState + "!");
        }

        boolean needSave = true;

        if (originalState.equals(ArrBulkActionRun.State.RUNNING)) {
            if (runningBulkActionWorkers.containsKey(bulkActionRun.getFundVersionId())) {
                AsyncBulkActionWorker bulkActionWorker = (AsyncBulkActionWorker) runningBulkActionWorkers.get(bulkActionRun.getFundVersionId());
                if (bulkActionWorker.getBulkActionRun().getBulkActionRunId().equals(bulkActionRun.getBulkActionRunId())) {
                    bulkActionWorker.terminate();
                    needSave = false;
                }
            }
        }

        if (needSave) {
            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);
            bulkActionRepository.save(bulkActionRun);
            bulkActionHelperService.eventPublishBulkAction(bulkActionRun);
            removeFromArrAsyncRequest(AsyncTypeEnum.BULK, bulkActionRun.getBulkActionRunId());
            bulkActionRepository.flush();
        }
    }

    public synchronized void terminateNodeWorkersByFund(Integer terminatedNodeFundId) {
        this.terminatedNodeFundId = terminatedNodeFundId;
    }

    /**
     * Volání z jednotlivých workerů po jejich skončení, plánování spuštění nových workerů
     * @param asyncRequestEvent
     */
    @Override
    @Transactional
    public void onApplicationEvent(AsyncRequestEvent asyncRequestEvent) {
        switch (asyncRequestEvent.getAsyncRequestVO().getType()) {
            case NODE:
                synchronized (this) {
                    AsyncRequestVO finishedRequestVO = asyncRequestEvent.getAsyncRequestVO();
                    deleteRequestFromRepository(finishedRequestVO.getRequestId());
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            addToLastHourRequests(new TimeRequestInfo(finishedRequestVO.getType(), System.currentTimeMillis()));
                            removeFromRunningNodeWorkers(finishedRequestVO.getFundVersionId(), finishedRequestVO.getRequestId());
                            runningNodeIdMap.remove(finishedRequestVO.getNodeId());
                            selecteNextWorker(finishedRequestVO.getType());
                        }
                    });
                    break;
                }
            case BULK:
                synchronized (this) {
                    AsyncRequestVO finishedRequestVO = asyncRequestEvent.getAsyncRequestVO();
                    deleteRequestFromRepository(finishedRequestVO.getRequestId());
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            addToLastHourRequests(new TimeRequestInfo(finishedRequestVO.getType(), System.currentTimeMillis()));
                            runningBulkActionWorkers.remove(finishedRequestVO.getFundVersionId());
                            runningBulkActionMap.remove(finishedRequestVO.getFundVersionId());
                            selecteNextWorker(finishedRequestVO.getType());
                        }
                    });
                    break;
                }
            case OUTPUT:
                synchronized (this) {
                    AsyncRequestVO finishedRequestVO = asyncRequestEvent.getAsyncRequestVO();
                    deleteRequestFromRepository(finishedRequestVO.getRequestId());
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            addToLastHourRequests(new TimeRequestInfo(finishedRequestVO.getType(), System.currentTimeMillis()));
                            runningOutputGeneratorWorkers.remove(finishedRequestVO.getFundVersionId());
                            selecteNextWorker(finishedRequestVO.getType());
                        }
                    });
                    break;
                }
        }
    }

    /**
     * vytváření statistiky pro LOAD
     */
    @Scheduled(fixedDelay = 1000)
    public void scheduledTask() {
        LocalDateTime now = LocalDateTime.now();
        int second = now.getSecond() + 60 * now.getMinute();
        threadLoadInfo.getNodeSlots()[second] = getRunningThreadsCount(AsyncTypeEnum.NODE);
        threadLoadInfo.getBulkSlots()[second] = getRunningThreadsCount(AsyncTypeEnum.BULK);
        threadLoadInfo.getBulkSlots()[second] = getRunningThreadsCount(AsyncTypeEnum.OUTPUT);
        logger.debug("Fixed delay task : " + System.currentTimeMillis());
    }

    /**
     * Vytváření statistiky pčekajících požadavků
     * @param type
     * @return
     */
    private int getWaitingRequests(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                int nodeWaitingRequests = 0;
                Iterator<Map.Entry<Integer, PriorityQueue<AsyncRequestVO>>> nodeIterator = nodeMap.entrySet().iterator();
                while (nodeIterator.hasNext()) {
                    nodeWaitingRequests += nodeIterator.next().getValue().size();
                }
                return nodeWaitingRequests;
            case BULK:
                int bulkWaitingRequests = 0;

                Iterator<Map.Entry<Integer, Queue<AsyncRequestVO>>> bulkActionIterator = bulkActionMap.entrySet().iterator();
                while (bulkActionIterator.hasNext()) {
                    bulkWaitingRequests += bulkActionIterator.next().getValue().size();
                }
                return bulkWaitingRequests;
            case OUTPUT:
                return outputQueue.size();
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Statistika pro počet aktuálně běžících vláken
     * @param type
     * @return
     */
    private int getRunningThreadsCount(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                return nodeTaskExecutor.getThreadPoolExecutor().getActiveCount();
            case BULK:
                return bulkActionTaskExecutor.getThreadPoolExecutor().getActiveCount();
            case OUTPUT:
                return outputTaskExecutor.getThreadPoolExecutor().getActiveCount();
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Statistika pro celkový počet vláken
     * @param type
     * @return
     */
    private int getThreadCount(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                return nodeTaskExecutor.getCorePoolSize();
            case BULK:
                return bulkActionTaskExecutor.getCorePoolSize();
            case OUTPUT:
                return outputTaskExecutor.getCorePoolSize();
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Vrácení detailních statistik podle jednotlivých typů požadavků
     * @param type
     * @return
     */
    public List<FundStatisticsVO> getFundStatistics(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                Collections.sort(nodeFundStatistics, Collections.reverseOrder());
                return nodeFundStatistics.subList(0, Math.min(nodeFundStatistics.size(), 100));
            case BULK:
                Collections.sort(bulkFundStatistics, Collections.reverseOrder());
                return bulkFundStatistics.subList(0, Math.min(bulkFundStatistics.size(), 100));
            case OUTPUT:
                Collections.sort(outputFundStatistics, Collections.reverseOrder());
                return outputFundStatistics.subList(0, Math.min(outputFundStatistics.size(), 100));
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Získání běžících workerů
     * @param type
     * @return
     */
    private List<IAsyncWorker> getRunningWorkers(AsyncTypeEnum type) {
        switch (type) {
            case NODE:
                List<IAsyncWorker> runningList = new ArrayList<>();
                for (Map.Entry<Integer, List<IAsyncWorker>> entry : runningNodeWorkers.entrySet()) {
                    runningList.addAll(entry.getValue());
                }
                return runningList;
            case BULK:
                return new ArrayList<>(runningBulkActionWorkers.values());
            case OUTPUT:
                return new ArrayList<>(runningOutputGeneratorWorkers);
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Přepočítání zpracovaných požadavků za poslední hodinu
     * @return
     */
    private TypeRequestCount getLastHourRequests() {
        int nodeRequestCount = 0;
        int bulkRequestCount = 0;
        int outputRequestCount = 0;
        for (TimeRequestInfo requestInfo : lastHourRequests) {
            if (requestInfo.getType() == AsyncTypeEnum.NODE) {
                nodeRequestCount++;
            } else if (requestInfo.getType() == AsyncTypeEnum.BULK) {
                bulkRequestCount++;
            } else if (requestInfo.getType() == AsyncTypeEnum.OUTPUT) {
                outputRequestCount++;
            }
        }
        return new TypeRequestCount(nodeRequestCount, bulkRequestCount, outputRequestCount);
    }

    /**
     * Výpočet aktuálního LOADu
     * @param type
     * @return
     */
    private double getCurrentLoad(AsyncTypeEnum type) {
        double sum = 0;
        switch (type) {
            case NODE:
                for (int s : threadLoadInfo.getNodeSlots()) {
                    sum += s;
                }
                return sum / 3600;
            case BULK:
                for (int s : threadLoadInfo.getBulkSlots()) {
                    sum += s;
                }
                return sum / 3600;
            case OUTPUT:
                for (int s : threadLoadInfo.getOutputSlots()) {
                    sum += s;
                }
                return sum / 3600;
            default:
                throw new NotImplementedException("Typ requestu neni implementován: " + type);
        }
    }

    /**
     * Obecné informace o zpracování požadavků
     * @return
     */
    public List<ArrAsyncRequestVO> dispatcherInfo() {
        List<ArrAsyncRequestVO> infoList = new ArrayList<>();
        TypeRequestCount requestCount = getLastHourRequests();

        AsyncTypeEnum type = AsyncTypeEnum.NODE;
        ArrAsyncRequestVO nodeTypeInfo = new ArrAsyncRequestVO(type, getCurrentLoad(type), requestCount.getNodeRequestCount(),
                getWaitingRequests(type), getRunningThreadsCount(type), getThreadCount(type), getRunningWorkers(type));
        infoList.add(nodeTypeInfo);

        type = AsyncTypeEnum.BULK;
        ArrAsyncRequestVO bulkTypeInfo = new ArrAsyncRequestVO(type, getCurrentLoad(type), requestCount.getBulkRequestCount(),
                getWaitingRequests(type), getRunningThreadsCount(type), getThreadCount(type), getRunningWorkers(type));
        infoList.add(bulkTypeInfo);

        type = AsyncTypeEnum.OUTPUT;
        ArrAsyncRequestVO outputTypeInfo = new ArrAsyncRequestVO(type, getCurrentLoad(type), requestCount.getOutputRequestCount(),
                getWaitingRequests(type), getRunningThreadsCount(type), getThreadCount(type), getRunningWorkers(type));
        infoList.add(outputTypeInfo);

        return infoList;
    }

    /**
     * čekání na ukončení všech asynchronních požadavků - pro účely testování
     */
    public void waitForFinishAll() {
        int i = 0;
        do {
            i = 0;
            i += nodeMap.size();
            i += waitingNodeRequestMap.size();
            i += runningNodeIdMap.size();
            i += getRunningThreadsCount(AsyncTypeEnum.NODE);
            i += getRunningThreadsCount(AsyncTypeEnum.BULK);
            i += getRunningThreadsCount(AsyncTypeEnum.OUTPUT);
            logger.debug("Cekani na dokonceni workeru : " + i);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new IllegalArgumentException();
            }
        } while (i > 0);
    }
}
