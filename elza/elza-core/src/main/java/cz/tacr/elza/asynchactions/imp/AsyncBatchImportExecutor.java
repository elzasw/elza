package cz.tacr.elza.asynchactions.imp;

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

/**
 * Runs import batches off the request thread. A batch may touch several funds, so the per-fund
 * limit does not order this queue; one batch at a time is enforced by the pool instead.
 */
public class AsyncBatchImportExecutor extends AsyncExecutor {

    public AsyncBatchImportExecutor(final ThreadPoolTaskExecutor executor,
                                    final PlatformTransactionManager txManager,
                                    final ArrAsyncRequestRepository asyncRequestRepository,
                                    final ApplicationContext appCtx) {
        super(AsyncTypeEnum.BATCH_IMPORT, executor, new RequestQueue<>(IAsyncRequest::getCurrentId),
              txManager, asyncRequestRepository, appCtx, Integer.MAX_VALUE);
    }

    @Override
    protected Class<? extends IAsyncWorker> workerClass() {
        return AsyncBatchImportWorker.class;
    }

    @Override
    protected IAsyncRequest readRequest(ArrAsyncRequest request) {
        return new AsyncRequest(request);
    }
}
