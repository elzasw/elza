package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdRequestIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Servisní třída pro obsluhu a správu požadavků
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Service
public class RequestQueueService implements ListenableFutureCallback<RequestQueueService.RequestExecute> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    @Qualifier("threadPoolTaskExecutorRQ")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    public void sendRequest(ArrRequest request,
                            final ArrFundVersion fundVersion) {
        requestService.setRequestState(request, ArrRequest.State.OPEN, ArrRequest.State.QUEUED);
        addRequestToQueue(request, fundVersion);
    }

    private void addRequestToQueue(ArrRequest request,
                                   ArrFundVersion fundVersion) {
        ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_REQUEST_QUEUE);

        ArrRequestQueueItem requestQueueItem = new ArrRequestQueueItem();
        requestQueueItem.setCreateChange(createChange);
        requestQueueItem.setRequest(request);
        requestQueueItem.setSend(false);

        requestQueueItemRepository.save(requestQueueItem);
        sendNotification(fundVersion, request, requestQueueItem, EventType.REQUEST_ITEM_QUEUE_CREATE);

        executeNextRequest();
    }

    private void sendNotification(final ArrFundVersion fundVersion,
                                  final ArrRequest request,
                                  final ArrRequestQueueItem requestQueueItem,
                                  final EventType type) {
        EventIdRequestIdInVersion event = new EventIdRequestIdInVersion(type, fundVersion.getFundVersionId(),
                request.getRequestId(), requestQueueItem.getRequestQueueItemId());
        eventNotificationService.publishEvent(event);
    }

    public void removeRequestFromQueue(ArrRequest request,
                                       ArrFundVersion fundVersion) {
        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequestAndSend(request, false);
        if (requestQueueItem == null) {
            throw new BusinessException(ArrangementCode.REQUEST_NOT_FOUND_IN_QUEUE);
        }

        requestService.setRequestState(request, ArrRequest.State.QUEUED, ArrRequest.State.REJECTED);

        request.setRejectReason("Removed from queue"); // TODO: neměl by být text, co ale vyplnit?
        requestRepository.save(request);

        requestQueueItemRepository.delete(requestQueueItem);
        sendNotification(fundVersion, request, requestQueueItem, EventType.REQUEST_ITEM_QUEUE_DELETE);
    }

    private void executeNextRequest() {

        final RequestQueueService thiz = this;

        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {

                ArrRequestQueueItem queueItem = requestQueueItemRepository.findNext();

                if (queueItem != null) {
                    RequestExecute requestExecute = new RequestExecute(queueItem.getRequestQueueItemId());

                    ListenableFuture<RequestExecute> future = taskExecutor.submitListenable(requestExecute);
                    future.addCallback(thiz);

                    //eventPublishBulkAction(bulkActionWorker.getBulkActionRun());
                }
            }
        });
    }

    @Override
    public void onFailure(final Throwable throwable) {
        try {
            logger.error("onFailure", throwable);
            try {
                Thread.sleep(1000); // TODO: smazat
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            executeNextRequest();
        }
    }

    @Override
    public void onSuccess(final RequestExecute requestExecute) {
        try {
            if (requestExecute.getThrowable() == null) {
                logger.info("onSuccess", requestExecute);
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            executeNextRequest();
        }
    }

    class RequestExecute implements Callable<RequestExecute> {

        public static final int ERROR_LENGHT = 1000;

        private Integer requestQueueItemId;

        private Throwable throwable = null;

        public RequestExecute(final Integer requestQueueItemId) {
            this.requestQueueItemId = requestQueueItemId;
        }

        @Override
        public RequestExecute call() throws Exception {
            (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                    ArrRequestQueueItem queueItem = requestQueueItemRepository.findOne(requestQueueItemId);
                    try {
                        queueItem.setAttemptToSend(LocalDateTime.now());
                        execute(queueItem);
                    } catch (Exception e) {
                        throwable = e;
                        queueItem.setSend(false);
                        String error = extractError(e);
                        queueItem.setError(error);
                        requestQueueItemRepository.save(queueItem);
                    }
                }
            });
            return this;
        }

        private String extractError(final Exception e) {
            final StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append(e.getLocalizedMessage()).append("\n");
            Throwable cause = e.getCause();
            while (cause != null && stringBuffer.length() < ERROR_LENGHT) {
                stringBuffer.append(cause.getLocalizedMessage()).append("\n");
                cause = cause.getCause();
            }
            return stringBuffer.length() > ERROR_LENGHT ? stringBuffer.substring(0, ERROR_LENGHT) : stringBuffer.toString();
        }

        private void execute(final ArrRequestQueueItem queueItem) {

            // TODO: dopsat odeslání požadavku a smazat kód
            if (1 > 0) {
                throw new NotImplementedException();
            }

            requestService.setRequestState(queueItem.getRequest(), ArrRequest.State.QUEUED, ArrRequest.State.SENT);
            queueItem.setSend(true);
            queueItem.setError(null);
            requestQueueItemRepository.save(queueItem);
        }

        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public String toString() {
            return "RequestExecute{" +
                    "requestQueueItemId=" + requestQueueItemId +
                    '}';
        }
    }

}
