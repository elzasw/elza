package cz.tacr.elza.asynchactions.nodevalid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.asynchactions.AsyncExecutor;
import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;

public class AsyncNodeExecutor extends AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncNodeExecutor.class);

    public AsyncNodeExecutor(final ThreadPoolTaskExecutor executor, final PlatformTransactionManager txManager,
                             final ArrAsyncRequestRepository asyncRequestRepository, final ApplicationContext appCtx,
                             final int maxPerFund) {
        super(AsyncTypeEnum.NODE,
                executor,
                new NodeValidationQueue(), txManager,
                asyncRequestRepository, appCtx, maxPerFund);
    }

    @Override
    protected Class<? extends IAsyncWorker> workerClass() {
        return AsyncNodeWorker.class;
    }

    @Override
    protected boolean skip(final IAsyncRequest request) {
        IAsyncRequest existAsyncRequest = queue.findById(request.getCurrentId());
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
            IAsyncRequest request = worker.getRequest();
            logger.error("Selhání requestu {}", request, error);
            countRequest();
            processing.removeIf(next -> next.getRequest().getRequestId().equals(request.getRequestId()));
            if (worker.getRequests().size() > 1) {
                for (IAsyncRequest r : worker.getRequests()) {
                    NodeValidationRequest nvr = (NodeValidationRequest) r;
                    nvr.setFailed(true);
                    enqueueInner(r);
                }
                resolveSkipped();
            } else {
                deleteRequests(worker.getRequests());
            }
            scheduleNext();
        }
    }

    @Override
    protected IAsyncRequest readRequest(ArrAsyncRequest request) {
        return new NodeValidationRequest(request);
    }
}