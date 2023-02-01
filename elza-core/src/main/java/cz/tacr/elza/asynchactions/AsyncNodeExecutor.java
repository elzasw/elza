package cz.tacr.elza.asynchactions;

import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.domain.AsyncTypeEnum;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;

public class AsyncNodeExecutor extends AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncNodeExecutor.class);

    public AsyncNodeExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager,
                             final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx,
                             final int maxPerFund) {
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