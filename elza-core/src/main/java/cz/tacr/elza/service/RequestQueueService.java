package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdRequestIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.ws.WsClient;
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
import java.util.Collections;
import java.util.List;
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
    private WsClient wsClient;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    public void sendRequest(final ArrRequest request,
                            final ArrFundVersion fundVersion) {
        validateRequest(request);
        requestService.setRequestState(request, ArrRequest.State.OPEN, ArrRequest.State.QUEUED);
        addRequestToQueue(request, fundVersion);
    }

    /**
     * Provede validaci požadavku.
     *
     * @param request požadavek
     */
    private void validateRequest(final ArrRequest request) {

        // kontroluje, že obsahuje alespoň jednu navázanou JP
        if (request instanceof ArrDigitizationRequest) {
            List<ArrDigitizationRequestNode> digitizationRequestNodes =
                    digitizationRequestNodeRepository.findByDigitizationRequest(Collections.singletonList((ArrDigitizationRequest) request));
            if (digitizationRequestNodes.size() == 0) {
                throw new BusinessException(ArrangementCode.REQUEST_INVALID);
            }
        }
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

    public boolean isRequestInQueue(ArrRequest request) {
        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequestAndSend(request, false);
        return requestQueueItem != null;
    }

    public void deleteRequestFromQueue(ArrRequest request,
                                       ArrFundVersion fundVersion) {
        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequestAndSend(request, false);
        if (requestQueueItem == null) {
            throw new BusinessException(ArrangementCode.REQUEST_NOT_FOUND_IN_QUEUE);
        }

        requestService.setRequestState(request, ArrRequest.State.QUEUED, ArrRequest.State.REJECTED);
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
            logger.error("Chyba při zpracování požadavku externím systémem", throwable);
        } finally {
            executeNextRequest();
        }
    }

    public List<ArrRequestQueueItem> findQueued() {
        return requestQueueItemRepository.findBySendOrderByCreateChangeAsc(false);
    }

    @Override
    public void onSuccess(final RequestExecute requestExecute) {
        try {
            if (requestExecute.getThrowable() == null) {
                logger.info("onSuccess", requestExecute);
            } else {
                logger.warn("notSend, wait for next try", requestExecute);
                try {
                    Thread.sleep(60 * 1000);
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

            List<ArrFundVersion> versions = queueItem.getRequest().getFund().getVersions();

            ArrFundVersion openVersion = null;
            for (ArrFundVersion version : versions) {
                if (version.getLockChange() == null) {
                    openVersion = version;
                    break;
                }
            }

            sendNotification(openVersion, queueItem.getRequest(), queueItem, EventType.REQUEST_ITEM_QUEUE_CHANGE);

            if (ArrRequest.ClassType.DIGITIZATION == queueItem.getRequest().getDiscriminator()) {
                ArrDigitizationRequest arrDigitizationRequest = (ArrDigitizationRequest) queueItem.getRequest();
                wsClient.postRequest(arrDigitizationRequest);
            } else if (ArrRequest.ClassType.DAO == queueItem.getRequest().getDiscriminator()) {
                ArrDaoRequest arrDaoRequest = (ArrDaoRequest) queueItem.getRequest();
                if (cz.tacr.elza.api.ArrDaoRequest.Type.DESTRUCTION == arrDaoRequest.getType()) {
                    wsClient.postDestructionRequest(arrDaoRequest);
                } else if (cz.tacr.elza.api.ArrDaoRequest.Type.DESTRUCTION == arrDaoRequest.getType()) {
                    wsClient.postTransferRequest(arrDaoRequest);
                } else {
                    throw new SystemException(BaseCode.SYSTEM_ERROR);
                }
            } else if (ArrRequest.ClassType.DAO_LINK == queueItem.getRequest().getDiscriminator()) {
                ArrDaoLinkRequest arrDaoLinkRequest = (ArrDaoLinkRequest) queueItem.getRequest();
                if (cz.tacr.elza.api.ArrDaoLinkRequest.Type.LINK == arrDaoLinkRequest.getType()) {
                    wsClient.onDaoLinked(arrDaoLinkRequest);
                } else if (cz.tacr.elza.api.ArrDaoLinkRequest.Type.UNLINK == arrDaoLinkRequest.getType()) {
                    wsClient.onDaoUnlinked(arrDaoLinkRequest);
                } else {
                    throw new SystemException(BaseCode.SYSTEM_ERROR);
                }
            } else {
                throw new SystemException(BaseCode.SYSTEM_ERROR);
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
