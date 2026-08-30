package cz.tacr.elza.service.da;

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
 * Carries out the steps of the actions over AIPs - one AIP per step.
 *
 * Steps run one at a time. The point of running them here rather than on the request thread is to
 * let the request be answered and to keep each AIP in a transaction of its own, not to get through
 * them faster; running them in parallel would only add ways for two steps of one action to get in
 * each other's way.
 */
public class AsyncAipExecutor extends AsyncExecutor {

    private final DaAipActionService actionService;

    public AsyncAipExecutor(final ThreadPoolTaskExecutor executor,
                            final PlatformTransactionManager txManager,
                            final ArrAsyncRequestRepository asyncRequestRepository,
                            final ApplicationContext appCtx,
                            final DaAipActionService actionService) {
        // An AIP that has no fund belongs to no fund version, and an action is often run over
        // exactly such an AIP, so the per-fund limit cannot order this queue. One step at a time
        // is enforced by the pool instead.
        super(AsyncTypeEnum.AIP, executor, new RequestQueue<>(IAsyncRequest::getCurrentId), txManager,
              asyncRequestRepository, appCtx, Integer.MAX_VALUE);
        this.actionService = actionService;
    }

    @Override
    protected boolean isFailedRequest(final ArrAsyncRequest request) {
        return actionService.abandonInterruptedStep(request.getAipActionItem());
    }

    @Override
    protected Class<? extends IAsyncWorker> workerClass() {
        return AsyncAipWorker.class;
    }

    @Override
    protected IAsyncRequest readRequest(ArrAsyncRequest request) {
        return new AsyncRequest(request);
    }
}
