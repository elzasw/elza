package cz.tacr.elza.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.SysExternalSystem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoDigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.ExternalSystemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdRequestIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.ws.types.v1.DaoIdentifiers;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;
import cz.tacr.elza.ws.types.v1.Materials;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;
import cz.tacr.elza.ws.types.v1.TransferRequest;

import static java.util.stream.Collectors.toList;

/**
 * Servisní třída pro obsluhu a správu požadavků
 */
@Service
public class RequestQueueService implements ListenableFutureCallback<RequestQueueService.RequestExecute> {

    private final static Logger logger = LoggerFactory.getLogger(RequestQueueService.class);

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DaoSyncService daoSyncService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    @Qualifier("threadPoolTaskExecutorRQ")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private FundVersionRepository fundVerRepos;

    @Autowired
    private WsClient wsClient;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    private ExternalSystemRepository externalSystemRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private DaoDigitizationRequestNodeRepository daoDigitizationRequestNodeRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final long RETRY_TIME = 60L * 1000L;

    // seznam identifikátorů externích systémů, na které se provádí odesílání požadavků
    private Set<Integer> externalSystemIds = new HashSet<>();
    private final Object lock = new Object();

    /**
     * Metoda pro obnovení/spuštění vláken pro zpracovávání fronty požadavků na externí systémy.
     */
    public void restartQueuedRequests() {
        List<SysExternalSystem> externalSystems = externalSystemRepository.findAll();
        for (SysExternalSystem externalSystem : externalSystems) {
            executeNextRequest(externalSystem.getExternalSystemId());
        }
    }

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

        if (request instanceof ArrDigitizationRequest) { // kontroluje, že obsahuje alespoň jednu navázanou JP
            List<ArrDigitizationRequestNode> digitizationRequestNodes =
                    digitizationRequestNodeRepository.findByDigitizationRequest(Collections.singletonList((ArrDigitizationRequest) request));
            if (digitizationRequestNodes.size() == 0) {
                throw new BusinessException("Neplatný požadavek, nejsou navázané žádné JP", ArrangementCode.REQUEST_INVALID);
            }
        } else if (request instanceof ArrDaoRequest) { // kontroluje, že obsahuje alespoň jedno navázané DAO
            if (daoRequestDaoRepository.countByDaoRequest((ArrDaoRequest) request) == 0) {
                throw new BusinessException("Neplatný požadavek, nejsou navázané žádné DAO", ArrangementCode.REQUEST_INVALID);
            }
        }
    }

    private void addRequestToQueue(final ArrRequest request,
                                   final ArrFundVersion fundVersion) {
        ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_REQUEST_QUEUE);

        ArrRequestQueueItem requestQueueItem = new ArrRequestQueueItem();
        requestQueueItem.setCreateChange(createChange);
        requestQueueItem.setRequest(request);
        requestQueueItem.setSend(false);
        requestQueueItem.setData(serializeRequest(request));

        requestQueueItemRepository.saveAndFlush(requestQueueItem);
        sendNotification(fundVersion, request, requestQueueItem, EventType.REQUEST_ITEM_QUEUE_CREATE);

        Integer externalSystemId;
        if (request instanceof ArrDaoRequest) {
            externalSystemId = ((ArrDaoRequest) request).getDigitalRepository().getExternalSystemId();
        } else if (request instanceof ArrDaoLinkRequest) {
            externalSystemId = ((ArrDaoLinkRequest) request).getDigitalRepository().getExternalSystemId();
        } else if (request instanceof ArrDigitizationRequest) {
            externalSystemId = ((ArrDigitizationRequest) request).getDigitizationFrontdesk().getExternalSystemId();
        } else {
            throw new NotImplementedException(request.getClass().getSimpleName());
        }

        executeNextRequest(externalSystemId);
    }

    /**
     * Sestavení požadavku připojení JP pro WS.
     *
     * @param request požadavek
     * @return WS požadavek
     */
    private OnDaoLinked createDaoLinked(final ArrDaoLinkRequest request) {
        OnDaoLinked daoLinked = new OnDaoLinked();
        daoLinked.setDaoIdentifier(request.getDao().getCode());
        daoLinked.setUsername(getUsername());
        daoLinked.setSystemIdentifier(request.getDigitalRepository().getElzaCode());
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByDaoAndDeleteChangeIsNull(request.getDao());
        if (CollectionUtils.isNotEmpty(daoLinks)) {
            final ArrDaoLink arrDaoLink = daoLinks.iterator().next();
            final ArrNode arrNode = arrDaoLink.getNode();
            final Did did = groovyScriptService.createDid(arrNode);
            daoLinked.setDid(did);
        }
        return daoLinked;
    }

    /**
     * Sestavení požadavku odpojení JP pro WS.
     *
     * @param request požadavek
     * @return WS požadavek
     */
    private OnDaoUnlinked createDaoUnlinked(final ArrDaoLinkRequest request) {
        final OnDaoUnlinked daoUnlinked = new OnDaoUnlinked();
        daoUnlinked.setDaoIdentifier(request.getDao().getCode());
        daoUnlinked.setUsername(getUsername());
        daoUnlinked.setSystemIdentifier(request.getDigitalRepository().getElzaCode());
        return daoUnlinked;
    }

    /**
     * Sestavení požadavku na delimitaci pro WS.
     *
     * @param request požadavek
     * @return WS požadavek
     */
    private TransferRequest createTransferRequest(final ArrDaoRequest request) {
        final TransferRequest transferRequest = new TransferRequest();
        transferRequest.setIdentifier(request.getCode());
        transferRequest.setDescription(request.getDescription());
        transferRequest.setUsername(getUsername());
        transferRequest.setSystemIdentifier(request.getDigitalRepository().getElzaCode());
        final DaoIdentifiers daoIdentifiers = new DaoIdentifiers();
        daoIdentifiers.getIdentifier().addAll(daoRequestDaoRepository.findDaoByDaoRequest(request).stream().map(dao -> dao.getCode()).collect(toList()));
        transferRequest.setDaoIdentifiers(daoIdentifiers);
        return transferRequest;
    }

    /**
     * Sestavení požadavku na skartaci pro WS.
     *
     * @param request požadavek
     * @return WS požadavek
     */
    private DestructionRequest createDestructionRequest(final ArrDaoRequest request) {
        DestructionRequest destructionRequest = new DestructionRequest();
        destructionRequest.setIdentifier(request.getCode());
        destructionRequest.setDescription(request.getDescription());
        destructionRequest.setUsername(getUsername());
        destructionRequest.setSystemIdentifier(request.getDigitalRepository().getElzaCode());
        final DaoIdentifiers daoIdentifiers = new DaoIdentifiers();
        daoIdentifiers.getIdentifier().addAll(daoRequestDaoRepository.findDaoByDaoRequest(request).stream().map(dao -> dao.getCode()).collect(toList()));
        destructionRequest.setDaoIdentifiers(daoIdentifiers);
        return destructionRequest;
    }

    /**
     * Sestavení požadavku na digitalizaci pro WS.
     *
     * @param request požadavek
     * @return WS požadavek
     */
    private DigitizationRequest createDigitizationRequest(final ArrDigitizationRequest request) {
        DigitizationRequest digitizationRequest = new DigitizationRequest();
        digitizationRequest.setIdentifier(request.getCode());
        digitizationRequest.setDescription(request.getDescription());
        digitizationRequest.setSystemIdentifier(request.getDigitizationFrontdesk().getElzaCode());
        Materials materials = new Materials();
        List<ArrDigitizationRequestNode> digitizationRequestNodes = daoDigitizationRequestNodeRepository.findByDigitizationRequest(request);
        for (ArrDigitizationRequestNode arrDigitizationRequestNode : digitizationRequestNodes) {
            final Did did = groovyScriptService.createDid(arrDigitizationRequestNode.getNode());
            materials.getDid().add(did);
        }
        digitizationRequest.setMaterials(materials);
        return digitizationRequest;
    }

    /**
     * Serializace požadavku.
     *
     * @param request požadavek
     * @return serializovaný požadavek
     */
    private String serializeRequest(final ArrRequest request) {
        Object data = getRequest(request);
        return serializeData(data);
    }

    private Object getRequest(ArrRequest request) {
        if (request instanceof ArrDigitizationRequest) {
            return createDigitizationRequest((ArrDigitizationRequest) request);
        } else if (request instanceof ArrDaoLinkRequest) {
            ArrDaoLinkRequest daoLinkRequest = (ArrDaoLinkRequest) request;
            switch (daoLinkRequest.getType()) {
                case LINK:
                    return createDaoLinked(daoLinkRequest);
                case UNLINK:
                    return createDaoUnlinked(daoLinkRequest);
                default:
                    throw new NotImplementedException("Neimplementovaný typ requestu pro link/unlink: " + daoLinkRequest.getType());
            }
        } else if (request instanceof ArrDaoRequest) {
            ArrDaoRequest daoRequest = (ArrDaoRequest) request;
            switch (daoRequest.getType()) {
                case DESTRUCTION:
                    return createDestructionRequest(daoRequest);
                case TRANSFER:
                    return createTransferRequest(daoRequest);
                case SYNC:
                    return daoSyncService.createDaosSyncRequest(daoRequest);
                default:
                    throw new NotImplementedException("Neimplementovaný typ requestu pro destruction/transfer/sync: " + daoRequest.getType());
            }
        } else {
            throw new NotImplementedException("Neimplementovaný typ requestu pro serializaci: " + request.getDiscriminator());
        }
    }

    private void sendNotification(final ArrFundVersion fundVersion,
                                  final ArrRequest request,
                                  final ArrRequestQueueItem requestQueueItem,
                                  final EventType type) {
        EventIdRequestIdInVersion event = new EventIdRequestIdInVersion(type, fundVersion.getFundVersionId(),
                request.getRequestId(), requestQueueItem.getRequestQueueItemId());
        eventNotificationService.publishEvent(event);
    }

    public boolean isRequestInQueue(final ArrRequest request) {
        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequestAndSend(request, false);
        return requestQueueItem != null;
    }

    public void deleteRequestFromQueue(final ArrRequest request,
                                       final ArrFundVersion fundVersion) {
        ArrRequestQueueItem requestQueueItem = requestQueueItemRepository.findByRequestAndSend(request, false);
        if (requestQueueItem == null) {
            throw new BusinessException("Požadavek " + request + " nenalezen ve frontě", ArrangementCode.REQUEST_NOT_FOUND_IN_QUEUE);
        }

        requestService.setRequestState(request, ArrRequest.State.QUEUED, ArrRequest.State.REJECTED);
        requestRepository.save(request);
        requestQueueItemRepository.delete(requestQueueItem);
        sendNotification(fundVersion, request, requestQueueItem, EventType.REQUEST_ITEM_QUEUE_DELETE);
    }

    private void executeNextRequest(final Integer externalSystemId) {

        final RequestQueueService thiz = this;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                try {
                    (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {

                        @Override
                        protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {

                            ArrRequestQueueItem queueItem = requestQueueItemRepository.findNext(externalSystemId);

                            if (queueItem != null) {
                                synchronized (lock) {
                                    if (!externalSystemIds.contains(externalSystemId)) {
                                        externalSystemIds.add(externalSystemId);
                                        RequestExecute requestExecute = new RequestExecute(queueItem.getRequestQueueItemId(), externalSystemId);
                                        ListenableFuture<RequestExecute> future = taskExecutor.submitListenable(requestExecute);
                                        future.addCallback(thiz);
                                    } else {
                                        logger.info("Externí systém " + externalSystemId + " již zpracovává požadavek");
                                    }
                                }
                            } else {
                                logger.info("Fronta pro externí systém " + externalSystemId + " je prazdná");
                            }
                        }
                    });
                } catch (Exception e) {
                    logger.error("Nastala chyba při předávání požadavku ke zpracování", e);
                }
            }
        });
    }

    @Override
    public void onFailure(final Throwable throwable) {
        logger.error("Chyba při zpracování požadavku externím systémem", throwable);
    }

    public List<ArrRequestQueueItem> findQueued() {
        return requestQueueItemRepository.findBySendOrderByCreateChangeAsc(false);
    }

    @Override
    public void onSuccess(final RequestExecute requestExecute) {
        try {
            if (requestExecute.getThrowable() == null) {
                logger.info("Úspěšné odeslání požadavku " + requestExecute);
            } else {
                logger.warn("Nepodařilo se odeslat požadavek " + requestExecute + " na externí systém " + requestExecute.externalSystemId);
                try {
                    Thread.sleep(RETRY_TIME);
                } catch (InterruptedException e) {
                    logger.error(e.toString(), e);
                    // nothing to do here
                    Thread.currentThread().interrupt();
                }
            }
            synchronized (lock) {
                externalSystemIds.remove(requestExecute.externalSystemId);
            }
        } finally {
            executeNextRequest(requestExecute.externalSystemId);
        }
    }

    /**
     * Deserializace objektu.
     *
     * @param data  data, podle kterých sestavujeme
     * @param clazz třída, kterou sestavujeme
     * @return vytvořený objekt
     */
    public static <T> T deserializeData(final String data, final Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu pro odeslání požadavku", e, BaseCode.JSON_PARSE);
        }
    }

    /**
     * Serializace objektu.
     *
     * @param data serializovaný objekt
     */
    public static String serializeData(final Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (IOException e) {
            throw new SystemException("Nastal problém při serializaci objektu pro odeslání požadavku", e, BaseCode.JSON_PARSE);
        }
    }

    private String getUsername() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail != null && userDetail.getId() != null) {
            return userDetail.getUsername();
        }
        return null;
    }

    class RequestExecute implements Callable<RequestExecute> {

        public static final int ERROR_LENGTH = 1000;

        private Integer requestQueueItemId;

        private Integer externalSystemId;

        private Throwable throwable = null;

        public RequestExecute(final Integer requestQueueItemId, final Integer externalSystemId) {
            this.requestQueueItemId = requestQueueItemId;
            this.externalSystemId = externalSystemId;
        }

        @Override
        public RequestExecute call() throws Exception {
            (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                    ArrRequestQueueItem queueItem = requestQueueItemRepository.findOne(requestQueueItemId);
                    if (queueItem != null) {
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
                    } else {
                        logger.error("Nebyl nalezen request queue item s id: " + requestQueueItemId);
                    }
                }
            });
            return this;
        }

        private String extractError(final Exception e) {
            final StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append(e.getLocalizedMessage()).append("\n");
            Throwable cause = e.getCause();
            while (cause != null && stringBuffer.length() < ERROR_LENGTH) {
                stringBuffer.append(cause.getLocalizedMessage()).append("\n");
                cause = cause.getCause();
            }
            return stringBuffer.length() > ERROR_LENGTH ? stringBuffer.substring(0, ERROR_LENGTH) : stringBuffer.toString();
        }

        private void execute(final ArrRequestQueueItem queueItem) {

            ArrRequest request = queueItem.getRequest();
            request = HibernateUtils.unproxy(request);

            // get active version
            ArrFundVersion openVersion = fundVerRepos.findByFundIdAndLockChangeIsNull(request.getFund().getFundId());
            if (openVersion == null) {
                throw new SystemException("Cannot find open version", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("fundId", request.getFund().getFundId());
            }

            sendNotification(openVersion, request, queueItem, EventType.REQUEST_ITEM_QUEUE_CHANGE);

            String externalSystemCode = null;

            if (ArrRequest.ClassType.DIGITIZATION == request.getDiscriminator()) {
                ArrDigitizationRequest arrDigitizationRequest = (ArrDigitizationRequest) request;
                DigitizationRequest digitizationRequest = deserializeData(queueItem.getData(), DigitizationRequest.class);
                externalSystemCode = wsClient.postRequest(digitizationRequest, arrDigitizationRequest.getDigitizationFrontdesk());
            } else if (ArrRequest.ClassType.DAO == request.getDiscriminator()) {
                ArrDaoRequest arrDaoRequest = (ArrDaoRequest) request;
                if (ArrDaoRequest.Type.DESTRUCTION == arrDaoRequest.getType()) {
                    DestructionRequest destructionRequest = deserializeData(queueItem.getData(), DestructionRequest.class);
                    externalSystemCode = wsClient.postDestructionRequest(destructionRequest, arrDaoRequest.getDigitalRepository());
                } else if (ArrDaoRequest.Type.TRANSFER == arrDaoRequest.getType()) {
                    TransferRequest transferRequest = deserializeData(queueItem.getData(), TransferRequest.class);
                    externalSystemCode = wsClient.postTransferRequest(transferRequest, arrDaoRequest.getDigitalRepository());
                } else if (ArrDaoRequest.Type.SYNC == arrDaoRequest.getType()) {
                    DaosSyncRequest daosSyncRequest = deserializeData(queueItem.getData(), DaosSyncRequest.class);
                    DaosSyncResponse daosSyncResponse = wsClient.syncDaos(daosSyncRequest, arrDaoRequest.getDigitalRepository());
                    daoSyncService.processDaosSyncResponse(daosSyncResponse);
                } else {
                    throw new SystemException("Neplatný typ: " + arrDaoRequest.getType(), BaseCode.SYSTEM_ERROR);
                }
            } else if (ArrRequest.ClassType.DAO_LINK == request.getDiscriminator()) {
                ArrDaoLinkRequest arrDaoLinkRequest = (ArrDaoLinkRequest) request;
                if (ArrDaoLinkRequest.Type.LINK == arrDaoLinkRequest.getType()) {
                    OnDaoLinked daoLinked = deserializeData(queueItem.getData(), OnDaoLinked.class);
                    wsClient.onDaoLinked(daoLinked, arrDaoLinkRequest.getDigitalRepository());
                } else if (ArrDaoLinkRequest.Type.UNLINK == arrDaoLinkRequest.getType()) {
                    OnDaoUnlinked daoUnlinked = deserializeData(queueItem.getData(), OnDaoUnlinked.class);
                    wsClient.onDaoUnlinked(daoUnlinked, arrDaoLinkRequest.getDigitalRepository());
                } else {
                    throw new SystemException("Neplatný typ: " + arrDaoLinkRequest.getType(), BaseCode.SYSTEM_ERROR);
                }
            } else {
                throw new SystemException("Neplatný objekt: " + request.getDiscriminator(), BaseCode.SYSTEM_ERROR);
            }

            request.setExternalSystemCode(externalSystemCode);

            requestService.setRequestState(request, ArrRequest.State.QUEUED, ArrRequest.State.SENT);
            queueItem.setSend(true);
            queueItem.setData(null); // odstraněnní dat (kvůli zbytečnému plnění DB)
            queueItem.setError(null);
            requestRepository.save(request);
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
