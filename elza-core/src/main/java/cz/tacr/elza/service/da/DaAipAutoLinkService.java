package cz.tacr.elza.service.da;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.config.da.DaAutoLinkConfig;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Attaches received AIPs to existing nodes without user interaction
 * ({@link cz.tacr.elza.api.DaOnReceivedAction#DOWNLOAD_METADATA}).
 *
 * The candidate nodes are the nodes of the AIP's fund (from PACKAGE-INFO) whose description
 * item configured in {@link DaAutoLinkConfig} carries one of the AIP identifiers - the
 * {@code unitid} values of the archived units in the EAD ({@link AipIdentifiers}) or the AIP
 * code itself. The AIP is attached only when exactly one node matches; no match and an
 * ambiguous match are logged and left for manual processing. An AIP that already has a live
 * link is never touched.
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
    private DescItemRepository descItemRepository;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private EventNotificationService eventNotificationService;
    @Autowired
    private DaAutoLinkConfig config;

    /**
     * Attaches every AIP whose identifiers select exactly one node. A failure of one AIP is
     * logged and does not stop the others - the metadata import itself already succeeded.
     *
     * @param identifiersByAip AIP id to the identifiers read from its metadata
     * @return number of created links
     */
    public int linkReceivedAips(Map<Integer, Set<String>> identifiersByAip) {
        int linked = 0;
        for (Map.Entry<Integer, Set<String>> entry : identifiersByAip.entrySet()) {
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
     * @param aipId       AIP to attach
     * @param identifiers identifiers of the archived units read from the AIP metadata
     * @return the created link, empty when the AIP was not attached
     */
    @Transactional
    public Optional<ArrDaLink> linkReceivedAip(Integer aipId, Set<String> identifiers) {
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

        Set<String> values = new LinkedHashSet<>();
        if (identifiers != null) {
            identifiers.stream().filter(StringUtils::isNotBlank).forEach(values::add);
        }
        if (StringUtils.isNotBlank(aip.getCode())) {
            values.add(aip.getCode());
        }

        List<ArrDescItem> items = findIdentifierItems(aipState, values);
        Set<Integer> nodeIds = items.stream().map(ArrDescItem::getNodeId).collect(Collectors.toSet());
        if (nodeIds.isEmpty()) {
            logger.info("AIP={} matches no node of fund={} by identifiers {}", aipId,
                    aipState.getFund().getFundId(), values);
            return Optional.empty();
        }
        if (nodeIds.size() > 1) {
            logger.warn("AIP={} matches several nodes {} of fund={} by identifiers {}, automatic attachment skipped",
                    aipId, nodeIds, aipState.getFund().getFundId(), values);
            return Optional.empty();
        }

        Integer nodeId = nodeIds.iterator().next();
        ArrDaLink link = daService.connectToJP(nodeId, aipId);
        logger.info("AIP={} automatically attached to node={}", aipId, nodeId);

        ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFund(aipState.getFund());
        if (fundVersion != null) {
            eventNotificationService.publishEvent(new EventIdNodeIdInVersion(EventType.DAO_LINK_CREATE,
                    fundVersion.getFundVersionId(), link.getDaoLinkId(), Collections.singletonList(nodeId)));
        }
        return Optional.of(link);
    }

    private List<ArrDescItem> findIdentifierItems(DaAipState aipState, Set<String> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        StaticDataProvider sdp = staticDataService.getData();
        ItemType itemType = sdp.getItemTypeByCode(config.getItemType());
        if (itemType == null) {
            logger.warn("Item type {} (elza.da.auto-link.item-type) does not exist, automatic attachment skipped",
                    config.getItemType());
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(config.getItemSpec())) {
            return descItemRepository.findOpenByFundTypeAndStringValues(aipState.getFund(), itemType.getEntity(), values);
        }
        RulItemSpec itemSpec = itemType.getItemSpecByCode(config.getItemSpec());
        if (itemSpec == null) {
            logger.warn("Item specification {} (elza.da.auto-link.item-spec) does not exist for type {}, automatic attachment skipped",
                    config.getItemSpec(), config.getItemType());
            return Collections.emptyList();
        }
        return descItemRepository.findOpenByFundTypeSpecAndStringValues(aipState.getFund(), itemType.getEntity(),
                itemSpec, values);
    }
}
