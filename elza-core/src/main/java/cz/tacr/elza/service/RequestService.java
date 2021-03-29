package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoLinkRequest.Type;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoLinkRequestRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DaoRequestRepository;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdDaoIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

import static java.util.stream.Collectors.toList;

/**
 * Servisní třída pro obsluhu a správu požadavků
 *
 * @since 07.12.2016
 */
@Service
public class RequestService {

    @Autowired
    private DigitizationRequestRepository digitizationRequestRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private DaoLinkRequestRepository daoLinkRequestRepository;

    @Autowired
    private DaoRequestRepository daoRequestRepository;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private RequestQueueService requestQueueService;

    @Autowired
    private FundVersionRepository fundVerRepos;

    /**
     * Vytvoření jednoznačného identifikátoru požadavku.
     *
     * @return jednoznačný identifikátor
     */
    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDigitizationRequest createDigitizationRequest(@NotNull final List<ArrNode> nodes,
                                                            @NotNull final ArrDigitizationFrontdesk digitizationFrontdesk,
                                                            @Nullable final String description,
                                                            @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        ArrDigitizationRequest digitizationRequest = new ArrDigitizationRequest();
        digitizationRequest.setCode(generateCode());
        digitizationRequest.setDescription(description);

        digitizationRequest.setDigitizationFrontdesk(digitizationFrontdesk);
        digitizationRequest.setFund(fundVersion.getFund());
        digitizationRequest.setCreateChange(arrangementInternalService.createChange(ArrChange.Type.CREATE_DIGI_REQUEST));
        digitizationRequest.setState(ArrRequest.State.OPEN);

        List<ArrDigitizationRequestNode> requestNodes = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrDigitizationRequestNode requestNode = new ArrDigitizationRequestNode();
            requestNode.setNode(node);
            requestNode.setDigitizationRequest(digitizationRequest);
            requestNodes.add(requestNode);
        }

        digitizationRequestRepository.save(digitizationRequest);
        digitizationRequestNodeRepository.saveAll(requestNodes);

        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CREATE, nodes);

        return digitizationRequest;
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDaoRequest createDaoRequest(@NotNull final List<ArrDao> daos,
                                          @Nullable final String description,
                                          @NotNull final ArrDaoRequest.Type type,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                          @NotNull final ArrDigitalRepository digitalRepository) {
        ArrDaoRequest daoRequest = new ArrDaoRequest();
        daoRequest.setCode(generateCode());
        daoRequest.setDescription(description);
        daoRequest.setDigitalRepository(digitalRepository);
        daoRequest.setFund(fundVersion.getFund());
        daoRequest.setCreateChange(arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_REQUEST));
        daoRequest.setState(ArrRequest.State.OPEN);
        daoRequest.setType(type);

        List<ArrDaoRequestDao> daosInOtherRequests = daoRequestDaoRepository.findByDaoAndState(daos, Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));
        if (daosInOtherRequests.size() != 0) {
            throw new BusinessException("Existuje požadavek, který již obsahuje přidávaný digitalizát(y) a není v uzavřeném stavu.", ArrangementCode.ALREADY_ADDED);
        }

        List<ArrDaoRequestDao> requestDaos = new ArrayList<>(daos.size());
        for (ArrDao dao : daos) {
            if (!dao.getDaoPackage().getDigitalRepository().getExternalSystemId().equals(digitalRepository.getExternalSystemId())) {
                throw new BusinessException("Požadavek má jiné uložiště než má dao", ArrangementCode.INVALID_REQUEST_DIGITAL_REPOSITORY_DAO);
            }
            ArrDaoRequestDao requestDao = new ArrDaoRequestDao();
            requestDao.setDao(dao);
            requestDao.setDaoRequest(daoRequest);
            requestDaos.add(requestDao);
        }

        daoRequestRepository.save(daoRequest);
        daoRequestDaoRepository.saveAll(requestDaos);

        sendDaoNotification(fundVersion, daoRequest, EventType.REQUEST_DAO_CREATE, daos);

        return daoRequest;
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDaoLinkRequest createDaoLinkRequest(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                  final ArrDao dao, final ArrChange change, final Type type, final ArrNode node) {
        final ArrDaoLinkRequest request = new ArrDaoLinkRequest();
        request.setCreateChange(change);
        request.setDao(dao);
        request.setType(type);
        request.setCode(generateCode());
        request.setState(ArrRequest.State.OPEN);
        request.setDidCode(node.getUuid());
        request.setDigitalRepository(dao.getDaoPackage().getDigitalRepository());
        request.setFund(fundVersion.getFund());
        return daoLinkRequestRepository.save(request);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void addNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                           @NotNull final List<ArrNode> nodes,
                                           @NotNull final ArrDigitizationFrontdesk digitizationFrontdesk,
                                           @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final String description) {
        if (!digitizationRequest.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException("Neplatný stav požadavku " + digitizationRequest + ": " + digitizationRequest.getState(), ArrangementCode.REQUEST_INVALID_STATE).set("state", digitizationRequest.getState());
        }

        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequestAndNode(digitizationRequest, nodes);
        if (digitizationRequestNodes.size() != 0) {
            throw new BusinessException("Požadavek již obsahuje přidávané JP", ArrangementCode.ALREADY_ADDED);
        }

        for (ArrNode node : nodes) {
            ArrDigitizationRequestNode requestNode = new ArrDigitizationRequestNode();
            requestNode.setNode(node);
            requestNode.setDigitizationRequest(digitizationRequest);
            digitizationRequestNodes.add(requestNode);
        }

        if (description != null) {
            digitizationRequest.setDescription(description);
        }

        digitizationRequest.setDigitizationFrontdesk(digitizationFrontdesk);

        digitizationRequestRepository.save(digitizationRequest);
        digitizationRequestNodeRepository.saveAll(digitizationRequestNodes);
        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CHANGE, nodes);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void addDaoDaoRequest(@NotNull final ArrDaoRequest daoRequest,
                                 @NotNull final List<ArrDao> daos,
                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                 @Nullable final String description) {
        if (!daoRequest.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException("Neplatný stav požadavku " + daoRequest + ": " + daoRequest.getState(), ArrangementCode.REQUEST_INVALID_STATE).set("state", daoRequest.getState());
        }

        List<ArrDaoRequestDao> daoRequestDaos = daoRequestDaoRepository.findByDaoRequestAndDao(daoRequest, daos);
        if (daoRequestDaos.size() != 0) {
            throw new BusinessException("Požadavek již obsahuje přidávaný digitalizát(y)", ArrangementCode.ALREADY_ADDED);
        }

        List<ArrDaoRequestDao> daosInOtherRequests = daoRequestDaoRepository.findByDaoAndState(daos, Arrays.asList(ArrRequest.State.OPEN, ArrRequest.State.QUEUED, ArrRequest.State.SENT));
        if (daosInOtherRequests.size() != 0) {
            throw new BusinessException("Existuje požadavek, který již obsahuje přidávaný digitalizát(y) a není v uzavřeném stavu.", ArrangementCode.ALREADY_ADDED);
        }

        for (ArrDao dao : daos) {
            if (!dao.getDaoPackage().getDigitalRepository().getExternalSystemId()
                    .equals(daoRequest.getDigitalRepository().getExternalSystemId())) {
                throw new BusinessException("Požadavek má jiné uložiště než má dao", ArrangementCode.INVALID_REQUEST_DIGITAL_REPOSITORY_DAO);
            }
            ArrDaoRequestDao requestDao = new ArrDaoRequestDao();
            requestDao.setDao(dao);
            requestDao.setDaoRequest(daoRequest);
            daoRequestDaos.add(requestDao);
        }

        if (description != null) {
            daoRequest.setDescription(description);
        }

        daoRequestRepository.save(daoRequest);
        daoRequestDaoRepository.saveAll(daoRequestDaos);
        sendDaoNotification(fundVersion, daoRequest, EventType.REQUEST_DAO_CHANGE, daos);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void removeNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                              @NotNull final List<ArrNode> nodes,
                                              @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        if (!digitizationRequest.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException("Neplatný stav požadavku " + digitizationRequest + ": " + digitizationRequest.getState(), ArrangementCode.REQUEST_INVALID_STATE).set("state", digitizationRequest.getState());
        }

        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequestAndNode(digitizationRequest, nodes);
        if (digitizationRequestNodes.size() != nodes.size()) {
            throw new BusinessException("Požadavek již neobsahuje odebírané JP", ArrangementCode.ALREADY_REMOVED);
        }

        digitizationRequestNodeRepository.deleteAll(digitizationRequestNodes);
        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CHANGE, nodes);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void changeRequest(@NotNull final ArrRequest request,
                              @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                              @Nullable final String description) {
        if (!request.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException("Neplatný stav požadavku " + request + ": " + request.getState(), ArrangementCode.REQUEST_INVALID_STATE).set("state", request.getState());
        }
        List<ArrNode> nodes = null;
        switch (request.getDiscriminator()) {
            case DAO:
                ((ArrDaoRequest)request).setDescription(description);
                break;
            case DIGITIZATION:
                nodes = digitizationRequestNodeRepository.findNodesByDigitizationRequest((ArrDigitizationRequest) request);
                ((ArrDigitizationRequest)request).setDescription(description);
                break;
            default:
                break;
        }
        sendNotification(fundVersion, request, EventType.REQUEST_CHANGE, nodes);
    }

    public ArrDigitizationRequest getDigitizationRequest(final Integer id) {
        return digitizationRequestRepository.getOneCheckExist(id);
    }

    public ArrDaoRequest getDaoRequest(final Integer id) {
        return daoRequestRepository.getOneCheckExist(id);
    }

    public ArrRequest getRequest(final Integer id) {
        return requestRepository.getOneCheckExist(id);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrRequest> findRequests(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                         @Nullable final ArrRequest.State state,
                                         @Nullable final ArrRequest.ClassType type,
                                         @Nullable final String description,
                                         @Nullable final LocalDateTime fromDate,
                                         @Nullable final LocalDateTime toDate, final String subType) {
        return requestRepository.findRequests(fund, state, type, description, fromDate, toDate, subType);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void sendRequest(@NotNull final ArrRequest request,
                            @AuthParam(type = AuthParam.Type.FUND) final ArrFundVersion fundVersion) {
        requestQueueService.sendRequest(request, fundVersion);
        List<ArrNode> nodes = null;
        if (request.getDiscriminator() == ArrRequest.ClassType.DIGITIZATION) {
            nodes = digitizationRequestNodeRepository.findNodesByDigitizationRequest((ArrDigitizationRequest) request);
        }
        sendNotification(fundVersion, request, EventType.REQUEST_CHANGE, nodes);
    }

    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public void deleteRequest(@NotNull final ArrRequest request) {

        ArrFundVersion openVersion = fundVerRepos.findByFundIdAndLockChangeIsNull(request.getFund().getFundId());
        if (openVersion == null) {
            throw new SystemException("Cannot find open version", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("fundId", request.getFund().getFundId());
        }

        List<ArrNode> nodes = null;
        if (requestQueueService.isRequestInQueue(request)) {
            requestQueueService.deleteRequestFromQueue(request, openVersion);
        } else {
            switch (request.getDiscriminator()) {
                case DIGITIZATION: {
                    nodes = digitizationRequestNodeRepository.findNodesByDigitizationRequest((ArrDigitizationRequest) request);
                    digitizationRequestNodeRepository.deleteByDigitizationRequest((ArrDigitizationRequest) request);
                    requestRepository.delete(request);
                    break;
                }
                case DAO: {
                    daoRequestDaoRepository.deleteByDaoRequest((ArrDaoRequest) request);
                    requestRepository.delete(request);
                    break;
                }
                default:
                    throw new IllegalStateException("Neimplementovaný typ požadavku: " + request.getDiscriminator());
            }
            sendNotification(openVersion, request, EventType.REQUEST_DELETE, nodes);
        }
    }

    /**
     * Nastavit stav požadavku.
     *
     * @param request  požadavek
     * @param oldState původní stav požadavku
     * @param newState nastavovaný stav požadavku
     */
    public void setRequestState(final ArrRequest request,
                                 final ArrRequest.State oldState,
                                 final ArrRequest.State newState) {
        boolean success = requestRepository.setState(request, oldState, newState);
        if (!success) {
            throw new BusinessException("Neplatný stav požadavku " + request + ": " + request.getState(), ArrangementCode.REQUEST_INVALID_STATE).set("state", request.getState()).set("setState", newState);
        }
    }

    private void sendNotification(final ArrFundVersion fundVersion,
                                  final ArrRequest request,
                                  final EventType type,
                                  final List<ArrNode> nodes) {
        List<Integer> nodeIds = nodes != null ? nodes.stream().map(node -> node.getNodeId()).collect(toList()) : null;
        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(type, fundVersion.getFundVersionId(),
                request.getRequestId(), nodeIds);
        eventNotificationService.publishEvent(event);
    }

    private void sendDaoNotification(final ArrFundVersion fundVersion,
                                     final ArrRequest request,
                                     final EventType type,
                                     final List<ArrDao> daos) {
        List<Integer> daoIds = daos != null ? daos.stream().map(dao -> dao.getDaoId()).collect(toList()) : null;
        EventIdDaoIdInVersion event = new EventIdDaoIdInVersion(type, fundVersion.getFundVersionId(),
                request.getRequestId(), daoIds);
        eventNotificationService.publishEvent(event);
    }

    public Map<Integer, Set<ArrDigitizationRequest>> findDigitizationRequest(final Set<Integer> nodeIds, final ArrRequest.State state) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByNodeIds(nodeIds, state);

        Map<Integer, Set<ArrDigitizationRequest>> result = new HashMap<>();
        nodeIds.forEach(id -> result.put(id, new HashSet<>()));
        for (ArrDigitizationRequestNode digitizationRequestNode : digitizationRequestNodes) {
            Integer nodeId = digitizationRequestNode.getNode().getNodeId();
            Set<ArrDigitizationRequest> digitizationRequests = result.get(nodeId);
            digitizationRequests.add(digitizationRequestNode.getDigitizationRequest());
        }
        return result;
    }
}
