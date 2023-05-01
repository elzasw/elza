package cz.tacr.elza.asynchactions;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;

public abstract class AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    /**
     * Čekací interval při aktivní čekání.
     */
    public final int WAIT = 10;

    /**
     * Časové okno pro výpočet zatížení.
     */
    public final int LOAD_SEC = 3600;

    /**
     * Maximální počet záznamů pro načtení z DB při obnově fronty
     */
    public final int READ_MAX_COUNT = 10000;

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
    protected final Object lockQueue = new Object();

    /**
     * Fronta čekajících požadavků.
     */
    protected final IRequestQueue<IAsyncRequest> queue;

    /**
     * Seznam přeskočených požadavků na smazání.
     */
    final List<IAsyncRequest> skipped = new ArrayList<>();

    /**
     * Seznam probíhajících zpracování.
     */
    protected final List<IAsyncWorker> processing = new ArrayList<>();

    /**
     * Maximální počet souběžných zpracování v rámci jedné verze archivního souboru.
     */
    final int maxPerFundVersion;

    public AsyncExecutor(final AsyncTypeEnum type,
                         final ThreadPoolTaskExecutor executor,
                         final IRequestQueue<IAsyncRequest> queue,
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
    public int getWorkers() {
        // Vrátíme větší z hodnot
        // - corePoolSize je nastavená hodnota
        // - poolSize je aktuální hodnota
        return Math.max(executor.getCorePoolSize(), executor.getPoolSize());
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
            List<IAsyncRequest> results = new ArrayList<>();
            int p = 0;
            List<ArrAsyncRequest> requests;
            List<ArrAsyncRequest> deleteRequests = new ArrayList<>();
            do {
                requests = asyncRequestRepository.findRequestsByPriorityWithLimit(getType(), PageRequest.of(p,
                                                                                                            READ_MAX_COUNT));

                logger.info("Async requests fetched from DB, type: {}, count: {}, page: {}",
                            getType(), requests.size(), p);
                for (ArrAsyncRequest request : requests) {
                    if (isFailedRequest(request)) {
                        deleteRequests.add(request);
                        logger.debug("Bude odstraněn požadavek z fronty z důvodu jeho chybového stavu. ID: {}", request
                                .getAsyncRequestId());
                    } else {
                        IAsyncRequest ar = readRequest(request);
                        results.add(ar);
                    }
                }
                p++;
            } while (requests.size() == READ_MAX_COUNT);

            if (deleteRequests.size() > 0) {
                asyncRequestRepository.deleteAll(deleteRequests);
            }

            logger.info("Obnovení databázové fronty {} - obnoveno: {}, odebráno: {}", getType(), results.size(),
                        deleteRequests.size());
            if (results.size() > 0) {
                enqueue(results);
            }
        });
    }

    /**
     * Read DB request and prepare executor specific request
     * 
     * @param request
     * @return
     */
    protected abstract IAsyncRequest readRequest(ArrAsyncRequest request);

    public AsyncTypeEnum getType() {
        return this.type;
    }

    protected void deleteRequest(IAsyncRequest request) {
        deleteRequests(Collections.singletonList(request));
    }

    protected void deleteRequests(Collection<? extends IAsyncRequest> requests) {
        tx(() -> {
            for (IAsyncRequest request : requests) {
                logger.debug("Mazání requestu z DB: {}", request);
                asyncRequestRepository.deleteByRequestId(request.getRequestId());
            }
        });
    }

    public void countRequest(int cnt) {
        TimeRequestInfo tri = new TimeRequestInfo();
        for (int i = 0; i < cnt; i++) {
            lastHourRequests.add(tri);
        }
        lastHourRequests.removeIf(toDelete -> System.currentTimeMillis() - toDelete.getTimeFinished() > 3600000);
    }

    public void countRequest() {
        lastHourRequests.add(new TimeRequestInfo());
        lastHourRequests.removeIf(toDelete -> System.currentTimeMillis() - toDelete.getTimeFinished() > 3600000);
    }

    public void onFail(IAsyncWorker worker, final Throwable error) {
        IAsyncRequest request = worker.getRequest();
        logger.error("Request failed: {}", request, error);
        List<? extends IAsyncRequest> requests = worker.getRequests();

        synchronized (lockQueue) {
            countRequest(requests.size());
            processing.remove(worker);
            deleteRequests(requests);
            scheduleNext();
        }
    }

    public void onSuccess(IAsyncWorker worker) {
        List<? extends IAsyncRequest> requests = worker.getRequests();
        logger.debug("Finished requests({}): {}", requests.size(), requests);

        synchronized (lockQueue) {
            countRequest(requests.size());
            processing.remove(worker);
            deleteRequests(requests);
            scheduleNext();
        }
    }

    public void terminate(Integer currentId) {
        List<IAsyncWorker> terminateWorkers = new ArrayList<>();
        synchronized (lockQueue) {
            Iterator<? extends IAsyncRequest> iterator = queue.iterator();
            List<IAsyncRequest> removed = new ArrayList<>();
            while (iterator.hasNext()) {
                IAsyncRequest request = iterator.next();
                if (currentId.equals(request.getCurrentId())) {
                    iterator.remove();
                    removed.add(request);
                }
            }
            if (removed.size() > 0) {
                deleteRequests(removed);
            }
            for (IAsyncWorker worker : processing) {
                IAsyncRequest request = worker.getRequest();
                if (currentId.equals(request.getCurrentId())) {
                    terminateWorkers.add(worker);
                }
            }
        }
        for (IAsyncWorker worker : terminateWorkers) {
            IAsyncRequest request = worker.getRequest();
            logger.debug("Ukončuji {} request: {}", getType(), request.getRequestId());
            worker.terminate();
        }
    }

    public void terminateFund(Integer fundVersionId) {
        List<IAsyncWorker> terminateWorkers = new ArrayList<>();
        synchronized (lockQueue) {
            Iterator<? extends IAsyncRequest> iterator = queue.iterator();
            List<IAsyncRequest> removed = new ArrayList<>();
            while (iterator.hasNext()) {
                IAsyncRequest next = iterator.next();
                if (fundVersionId.equals(next.getFundVersionId())) {
                    iterator.remove();
                    removed.add(next);
                }
            }
            if (removed.size() > 0) {
                deleteRequests(removed);
            }
            for (IAsyncWorker worker : processing) {
                IAsyncRequest request = worker.getRequest();
                if (fundVersionId.equals(request.getFundVersionId())) {
                    terminateWorkers.add(worker);
                }
            }
        }
        for (IAsyncWorker worker : terminateWorkers) {
            IAsyncRequest request = worker.getRequest();
            logger.debug("Ukončuji {} request: {}", getType(), request.getRequestId());
            worker.terminate();
        }
    }

    protected boolean isEmptyWorker() {
        return processing.size() < getWorkers();
    }

    // TODO: what is it
    @Nullable
    protected List<? extends IAsyncRequest> selectNext() {
        Map<Integer, Integer> fundVersionCount = calcFundVersionsPerWorkers();

        List<? extends IAsyncRequest> next;
        List<? extends IAsyncRequest> selected = null;
        List<IAsyncRequest> backToQueue = new ArrayList<>();
        while (CollectionUtils.isNotEmpty((next = queue.poll()))) {
            Integer fundVersionId = next.get(0).getFundVersionId();
            Integer count = fundVersionCount.getOrDefault(fundVersionId, 0);
            if (count < maxPerFundVersion) {
                selected = next;
                break;
            } else {
                backToQueue.addAll(next);
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
            IAsyncRequest request = asyncWorker.getRequest();
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

    public void scheduleNext() {
        while (!queue.isEmpty() && isEmptyWorker() && running.get()) {
            List<? extends IAsyncRequest> requests = selectNext();
            if (CollectionUtils.isNotEmpty(requests)) {
                IAsyncWorker worker = appCtx.getBean(workerClass(), requests);
                logger.debug("Naplánování requestů: {}", requests);
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
    public void enqueue(final IAsyncRequest request) {
        enqueue(Collections.singletonList(request));
    }

    /**
     * Vložení do fronty požadavku z DB
     * 
     * @param request
     */
    public void enqueue(ArrAsyncRequest request) {
        IAsyncRequest ar = readRequest(request);
        enqueue(ar);
    }

    public void enqueue(Iterable<ArrAsyncRequest> reqList) {
        // prepare logical objects
        List<IAsyncRequest> requests = new ArrayList<>();
        for (ArrAsyncRequest request : reqList) {
            IAsyncRequest ar = readRequest(request);
            requests.add(ar);
        }
        enqueue(requests);
    }

    protected void enqueueInner(final IAsyncRequest request) {
        if (request.getType() != getType()) {
            throw new IllegalStateException("Neplatný typ požadavku");
        }
        // detekce, zda-li se má požadavek přidat nebo přeskočit
        if (skip(request)) {
            skipped.add(request); // přidání do fronty na přeskočení
        } else {
            queue.add(request);
        }
    }

    /**
     * Přeskočit požadavek?
     *
     * @param request požadavek na zpracování
     * @return true - ano, přeskočit
     */
    protected boolean skip(final IAsyncRequest request) {
        return false;
    }

    /**
     * Synchronní čekání na dokončení všech požadavků.
     */
    public void waitForFinish() {
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
     * Hromadné přidání požadavků do paměťové fronty
     * 
     * Metoda naplánuje požadavky po commitu.
     *
     * @param requests
     *            požadavky na zpracování
     */
    public void enqueue(final Collection<IAsyncRequest> requests) {
        // přidáváme až po úspěšném dokončení probíhající transakce
        afterTx(() -> {
            synchronized (lockQueue) {
                for (IAsyncRequest asyncRequest : requests) {
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
    public void resolveSkipped() {
        if (skipped.size() > 0) {
            logger.debug("Přeskočeny požadavky z fronty {}: {}, {}", getType(), skipped.size(), skipped.stream()
                    .map(IAsyncRequest::getRequestId)
                    .collect(Collectors.toList()));
            deleteRequests(skipped);
            skipped.clear();
        }
    }

    /**
     * Zastavení zpracování.
     */
    public void stop() {
        logger.debug("Stopping queue, type: {}", this.type);

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
    public void start() {
        logger.debug("Starting queue, type: {}", this.type);

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
     * @param fundVersionId
     *            id verze archivní pomůcky
     * @return true - je zpracováno něco z verze fondu
     */
    public boolean isProcessing(final Integer fundVersionId) {
        synchronized (lockQueue) {
            for (IAsyncRequest request : queue) {
                if (fundVersionId.equals(request.getFundVersionId())) {
                    return true;
                }
            }
            for (IAsyncWorker worker : processing) {
                IAsyncRequest request = worker.getRequest();
                if (fundVersionId.equals(request.getFundVersionId())) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<IAsyncRequest> getCurrentRequests() {
        synchronized (lockQueue) {
            List<IAsyncRequest> ret = new ArrayList<>(queue.size());
            for (IAsyncRequest request : queue) {
                ret.add(request);
            }
            return ret;
        }
    }

    public int getQueueSize() {
        synchronized (lockQueue) {
            return queue.size();
        }
    }

    public int getProcessingSize() {
        synchronized (lockQueue) {
            return processing.size();
        }
    }

    public Collection<IAsyncWorker> getProcessing() {
        // check that object is locked
        Validate.isTrue(Thread.holdsLock(lockQueue));
        return processing;
    }
}