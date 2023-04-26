package cz.tacr.elza.asynchactions.ap;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.asynchactions.AsyncExecutor;
import cz.tacr.elza.asynchactions.AsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IAsyncWorker;
import cz.tacr.elza.asynchactions.RequestQueue;
import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;
import cz.tacr.elza.repository.ArrAsyncRequestRepository;

public class AsyncAccessPointExecutor extends AsyncExecutor {

    public AsyncAccessPointExecutor(final ThreadPoolTaskExecutor executor,
                             final PlatformTransactionManager txManager,
                             final ArrAsyncRequestRepository asyncRequestRepository,
                             final ApplicationContext appCtx) {
        super(AsyncTypeEnum.AP,
                executor,
                new RequestQueue<>(IAsyncRequest::getCurrentId),
                txManager,
                asyncRequestRepository, appCtx, Integer.MAX_VALUE);
    }

    @Override
    protected Class<? extends IAsyncWorker> workerClass() {
        return AsyncAccessPointWorker.class;
    }

    @Override
    protected boolean skip(final IAsyncRequest request) {
        IAsyncRequest existAsyncRequest = queue.findById(request.getCurrentId());
        if (existAsyncRequest == null) {
            // neexistuje ve frontě, chceme přidat
            return false;
        } else {
            int priorityExists = existAsyncRequest.getPriority();
            int priorityAdding = request.getPriority();
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
    protected IAsyncRequest readRequest(ArrAsyncRequest request) {
        return new AsyncRequest(request);
    }
}