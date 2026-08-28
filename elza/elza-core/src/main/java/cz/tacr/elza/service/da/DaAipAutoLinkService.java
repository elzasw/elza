package cz.tacr.elza.service.da;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Attaches received AIPs to existing nodes without user interaction
 * ({@link cz.tacr.elza.api.DaOnReceivedAction#DOWNLOAD_METADATA}).
 *
 * The AIP is matched onto a node by UUID: the UUID of the package first, then the levels of
 * its logical structural map top down, then its representations - see {@link AipNodeUuids}.
 * The first UUID that belongs to a node of the AIP's fund wins, so the match is the outermost
 * part of the AIP that ELZA already describes. An AIP that already has a live link is never
 * touched.
 */
@Service
public class DaAipAutoLinkService {

    private static final Logger logger = LoggerFactory.getLogger(DaAipAutoLinkService.class);

    @Autowired
    private DaService daService;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private EventNotificationService eventNotificationService;

    /**
     * Attaches every AIP whose UUIDs select a node. A failure of one AIP is logged and does not
     * stop the others - the metadata import itself already succeeded.
     *
     * @param uuidsByAip AIP id to the UUIDs it offers, in matching order
     * @return number of created links
     */
    public int linkReceivedAips(Map<Integer, List<String>> uuidsByAip) {
        int linked = 0;
        for (Map.Entry<Integer, List<String>> entry : uuidsByAip.entrySet()) {
            try {
                if (linkReceivedAip(entry.getKey(), entry.getValue()).isPresent()) {
                    linked++;
                }
            } catch (Exception e) {
                logger.error("Automatic attachment of AIP={} failed", entry.getKey(), e);
            }
        }
        return linked;
    }

    /**
     * @param aipId     AIP to attach
     * @param nodeUuids UUIDs the AIP offers, in matching order
     * @return the created link, empty when the AIP was not attached
     */
    @Transactional
    public Optional<ArrDaLink> linkReceivedAip(Integer aipId, List<String> nodeUuids) {
        DaAip aip = daService.findAipById(aipId);
        if (!daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).isEmpty()) {
            logger.debug("AIP={} is already attached to a node, automatic attachment skipped", aipId);
            return Optional.empty();
        }
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        if (aipState == null || aipState.getFund() == null) {
            logger.info("AIP={} has no fund, automatic attachment skipped", aipId);
            return Optional.empty();
        }
        if (nodeUuids == null || nodeUuids.isEmpty()) {
            logger.info("AIP={} offers no UUID to match a node by", aipId);
            return Optional.empty();
        }

        Map<String, ArrNode> nodesByUuid = nodeRepository
                .findByFundAndUuidIn(aipState.getFund(), nodeUuids).stream()
                .collect(Collectors.toMap(ArrNode::getUuid, Function.identity(), (a, b) -> a));
        if (nodesByUuid.isEmpty()) {
            logger.info("AIP={} matches no node of fund={} by any of its {} UUID(s)", aipId,
                    aipState.getFund().getFundId(), nodeUuids.size());
            return Optional.empty();
        }

        // the UUIDs come in matching order, so the first hit is the outermost matching part
        String matchedUuid = nodeUuids.stream().filter(nodesByUuid::containsKey).findFirst().orElseThrow();
        ArrNode node = nodesByUuid.get(matchedUuid);
        if (nodesByUuid.size() > 1) {
            logger.info("AIP={} matches {} nodes of fund={}, attaching to the first one in matching order: {}",
                    aipId, nodesByUuid.size(), aipState.getFund().getFundId(), matchedUuid);
        }

        ArrDaLink link = daService.connectToJP(node.getNodeId(), aipId);
        logger.info("AIP={} automatically attached to node={} by UUID {}", aipId, node.getNodeId(), matchedUuid);

        ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFund(aipState.getFund());
        if (fundVersion != null) {
            eventNotificationService.publishEvent(new EventIdNodeIdInVersion(EventType.DAO_LINK_CREATE,
                    fundVersion.getFundVersionId(), link.getDaoLinkId(),
                    Collections.singletonList(node.getNodeId())));
        }
        return Optional.of(link);
    }
}
