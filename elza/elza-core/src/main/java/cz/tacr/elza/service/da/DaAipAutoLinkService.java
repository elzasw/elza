package cz.tacr.elza.service.da;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaDaoRepository;
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
 * part of the AIP that ELZA already describes. A UUID match always wins. An AIP that already has
 * a live link is never touched.
 *
 * What follows depends on what matched:
 * <ul>
 * <li>the package itself, or one of its representations - the whole AIP is attached to the node;
 * nothing is created, the description is taken to exist and be complete;</li>
 * <li>a level of the logical structural map - the levels below it that ELZA does not describe yet
 * are created, by the rules of the fund, with their items from the EAD, and the files are
 * attached to them ({@link DaImportService#importBelow}); the matched node itself is taken as it
 * is. When the rules of the fund cannot import packages, the whole AIP is attached instead.</li>
 * </ul>
 * A failure is recorded as a problem of the AIP; nothing of the AIP is attached then, so it can be
 * processed again once the problem is solved.
 */
@Service
public class DaAipAutoLinkService {

    private static final Logger logger = LoggerFactory.getLogger(DaAipAutoLinkService.class);

    /**
     * What the automatic processing did to an AIP.
     *
     * @param nodeId the node the AIP was matched onto
     * @param link the link of the whole AIP; null when the levels below the node were imported
     * @param imported what the import below the node did; null when the whole AIP was attached
     */
    public record AutoLink(Integer nodeId, @Nullable ArrDaLink link, @Nullable DaImportBuilder.Outcome imported) {
    }

    @Autowired
    private DaService daService;
    @Autowired
    private DaImportService daImportService;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private EventNotificationService eventNotificationService;
    @Autowired
    private DaAipReferenceResolver referenceResolver;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    /**
     * Attaches every AIP whose UUIDs select a node, each in a transaction of its own. A failure of
     * one AIP is recorded on it and does not stop the others - the metadata import itself already
     * succeeded.
     *
     * @param uuidsByAip AIP id to the UUIDs it offers, in matching order
     * @return number of attached AIPs
     */
    public int linkReceivedAips(Map<Integer, List<String>> uuidsByAip) {
        int linked = 0;
        for (Map.Entry<Integer, List<String>> entry : uuidsByAip.entrySet()) {
            Integer aipId = entry.getKey();
            try {
                Optional<AutoLink> result = new TransactionTemplate(txManager)
                        .execute(status -> linkReceivedAip(aipId, entry.getValue()));
                if (result != null && result.isPresent()) {
                    linked++;
                }
            } catch (Exception e) {
                logger.error("Automatic attachment of AIP={} failed", aipId, e);
                recordProblem(aipId, e);
            }
        }
        return linked;
    }

    /** The work of the failed attachment is rolled back; the problem is written in a transaction of its own. */
    private void recordProblem(Integer aipId, Exception failure) {
        try {
            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(daService.findAipById(aipId));
                if (aipState != null) {
                    referenceResolver.recordProblem(aipState, AipProblem.of(failure));
                    aipStateRepository.save(aipState);
                }
            });
        } catch (Exception e) {
            logger.error("Problem of the automatic attachment of AIP={} could not be recorded", aipId, e);
        }
    }

    /**
     * @param aipId     AIP to attach
     * @param nodeUuids UUIDs the AIP offers, in matching order
     * @return what was done, empty when the AIP was not attached
     */
    @Transactional
    public Optional<AutoLink> linkReceivedAip(Integer aipId, List<String> nodeUuids) {
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

        if (!matchedUuid.equals(AipNodeUuids.normalize(aip.getCode()))) {
            Optional<DaImportBuilder.Outcome> imported = daImportService.importBelow(aip, node, matchedUuid, false);
            if (imported.isPresent()) {
                if (imported.get().attached() == 0) {
                    attachMatchedLevel(aip, node, matchedUuid);
                }
                logger.info("AIP={} matched node={} by UUID {}, levels below imported: {}", aipId,
                        node.getNodeId(), matchedUuid, imported.get());
                return Optional.of(new AutoLink(node.getNodeId(), null, imported.get()));
            }
        }

        ArrDaLink link = daService.connectToJP(node.getNodeId(), aipId);
        logger.info("AIP={} automatically attached to node={} by UUID {}", aipId, node.getNodeId(), matchedUuid);

        ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFund(aipState.getFund());
        if (fundVersion != null) {
            eventNotificationService.publishEvent(new EventIdNodeIdInVersion(EventType.DAO_LINK_CREATE,
                    fundVersion.getFundVersionId(), link.getDaoLinkId(),
                    Collections.singletonList(node.getNodeId())));
        }
        return Optional.of(new AutoLink(node.getNodeId(), link, null));
    }

    /**
     * Nothing below the matched level was attached - e.g. the matched level is the lowest one. So
     * that the AIP does not stay unattached, the matched level of the package is attached to the
     * node it matched.
     */
    private void attachMatchedLevel(DaAip aip, ArrNode node, String matchedUuid) {
        daoRepository.findByAipAndTypeAndDeleteChangeIsNull(aip, DaDao.DaoType.LOGICAL).stream()
                .filter(dao -> matchedUuid.equals(AipNodeUuids.normalize(dao.getCode())))
                .findFirst()
                .ifPresent(dao -> daService.connectPartToJP(node, aip, dao));
    }
}
