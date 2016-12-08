package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DigitizationRequestRepository;
import cz.tacr.elza.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
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
    private ArrangementService arrangementService;

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
                                                            @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) {
        ArrDigitizationRequest request = new ArrDigitizationRequest();
        request.setCode(generateCode());
        request.setDescription(description);
        List<ArrDigitizationFrontdesk> digitizationFrontdeskList = externalSystemService.findDigitizationFrontdesk();
        if (digitizationFrontdeskList.size() != 1) {
            throw new BusinessException(ArrangementCode.ILLEGAL_COUNT_EXTERNAL_SYSTEM);
        }
        request.setDigitizationFrontdesk(digitizationFrontdeskList.get(0));
        request.setFund(fund);
        request.setCreateChange(arrangementService.createChange(null));
        request.setState(ArrRequest.State.OPEN);

        List<ArrDigitizationRequestNode> requestNodes = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrDigitizationRequestNode requestNode = new ArrDigitizationRequestNode();
            requestNode.setNode(node);
            requestNode.setDigitizationRequest(request);
            requestNodes.add(requestNode);
        }

        digitizationRequestRepository.save(request);
        digitizationRequestNodeRepository.save(requestNodes);
        // TODO: websockety

        return request;
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void addNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                           @NotNull final List<ArrNode> nodes,
                                           @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) {

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

        digitizationRequestNodeRepository.save(digitizationRequestNodes);
        // TODO: websockety
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void removeNodeDigitizationRequest(@NotNull final ArrDigitizationRequest digitizationRequest,
                                              @NotNull final List<ArrNode> nodes,
                                              @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) {
        List<ArrDigitizationRequestNode> digitizationRequestNodes = digitizationRequestNodeRepository.findByDigitizationRequestAndNode(digitizationRequest, nodes);
        if (digitizationRequestNodes.size() != nodes.size()) {
            throw new BusinessException(ArrangementCode.ALREADY_REMOVED);
        }

        digitizationRequestNodeRepository.delete(digitizationRequestNodes);
        // TODO: websockety
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

    public void sendRequest(final ArrRequest request) {
        if (!request.getState().equals(ArrRequest.State.OPEN)) {
            throw new BusinessException(ArrangementCode.CANT_SEND);
        }

        // TODO: dopsat
    }
}
