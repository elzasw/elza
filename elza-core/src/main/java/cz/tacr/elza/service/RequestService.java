package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.RequestQueueItemRepository;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servisní třída pro obsluhu a správu požadavků
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Service
public class RequestService {

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private DigitizationRequestRepository digitizationRequestRepository;

    @Autowired
    private DigitizationRequestNodeRepository digitizationRequestNodeRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RequestQueueItemRepository requestQueueItemRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private RequestQueueService requestQueueService;

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
                                                            @Nullable final String description,
                                                            @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        ArrDigitizationRequest digitizationRequest = new ArrDigitizationRequest();
        digitizationRequest.setCode(generateCode());
        digitizationRequest.setDescription(description);
        List<ArrDigitizationFrontdesk> digitizationFrontdeskList = externalSystemService.findDigitizationFrontdesk();
        if (digitizationFrontdeskList.size() != 1) {
            throw new BusinessException(ArrangementCode.ILLEGAL_COUNT_EXTERNAL_SYSTEM);
        }
        digitizationRequest.setDigitizationFrontdesk(digitizationFrontdeskList.get(0));
        digitizationRequest.setFund(fundVersion.getFund());
        digitizationRequest.setCreateChange(arrangementService.createChange(ArrChange.Type.CREATE_DIGI_REQUEST));
        digitizationRequest.setState(ArrRequest.State.OPEN);

        List<ArrDigitizationRequestNode> requestNodes = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrDigitizationRequestNode requestNode = new ArrDigitizationRequestNode();
            requestNode.setNode(node);
            requestNode.setDigitizationRequest(digitizationRequest);
            requestNodes.add(requestNode);
        }

        digitizationRequestRepository.save(digitizationRequest);
        digitizationRequestNodeRepository.save(requestNodes);

        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CREATE, nodes);

        return digitizationRequest;
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void addNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                           @NotNull final List<ArrNode> nodes,
                                           @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final String description) {

        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequestAndNode(digitizationRequest, nodes);
        if (digitizationRequestNodes.size() != 0) {
            throw new BusinessException(ArrangementCode.ALREADY_ADDED);
        }

        for (ArrNode node : nodes) {
            ArrDigitizationRequestNode requestNode = new ArrDigitizationRequestNode();
            requestNode.setNode(node);
            requestNode.setDigitizationRequest(digitizationRequest);
            digitizationRequestNodes.add(requestNode);
        }

        digitizationRequest.setDescription(description);
        digitizationRequestRepository.save(digitizationRequest);
        digitizationRequestNodeRepository.save(digitizationRequestNodes);
        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CHANGE, nodes);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void removeNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                              @NotNull final List<ArrNode> nodes,
                                              @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequestAndNode(digitizationRequest, nodes);
        if (digitizationRequestNodes.size() != nodes.size()) {
            throw new BusinessException(ArrangementCode.ALREADY_REMOVED);
        }

        digitizationRequestNodeRepository.delete(digitizationRequestNodes);
        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CHANGE, nodes);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void changeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                          @Nullable final String description) {
        if (!digitizationRequest.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException(ArrangementCode.REQUEST_INVALID_STATE).set("state", digitizationRequest.getState());
        }
        digitizationRequest.setDescription(description);
        sendNotification(fundVersion, digitizationRequest, EventType.REQUEST_CHANGE, null);
    }

    public ArrDigitizationRequest getDigitizationRequest(final Integer id) {
        return digitizationRequestRepository.getOneCheckExist(id);
    }

    public ArrRequest getRequest(final Integer id) {
        return requestRepository.getOneCheckExist(id);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrRequest> findRequests(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund,
                                         @Nullable final ArrRequest.State state,
                                         @Nullable final ArrRequest.ClassType type) {
        return requestRepository.findRequests(fund, state, type);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void sendRequest(@NotNull final ArrRequest request,
                            @AuthParam(type = AuthParam.Type.FUND) final ArrFundVersion fundVersion) {
        requestQueueService.sendRequest(request, fundVersion);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void removeQueuedRequest(@NotNull final ArrRequest request,
                                    @AuthParam(type = AuthParam.Type.FUND) final ArrFundVersion fundVersion) {
        requestQueueService.removeRequestFromQueue(request, fundVersion);
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
            throw new BusinessException(ArrangementCode.REQUEST_INVALID_STATE).set("state", request.getState()).set("setState", newState);
        }
    }

    private void sendNotification(final ArrFundVersion fundVersion,
                                  final ArrDigitizationRequest digitizationRequest,
                                  final EventType type,
                                  final List<ArrNode> nodes) {
        List<Integer> nodeIds = nodes != null ? new ArrayList<>(nodes.size()) : null;

        if (nodes != null) {
            nodes.forEach(node -> nodeIds.add(node.getNodeId()));
        }

        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(type, fundVersion.getFundVersionId(),
                digitizationRequest.getRequestId(), nodeIds);
        eventNotificationService.publishEvent(event);
    }

    public Map<Integer, Set<ArrDigitizationRequest>> findDigitizationRequest(final Set<Integer> nodeIds, final ArrRequest.State state) {
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
