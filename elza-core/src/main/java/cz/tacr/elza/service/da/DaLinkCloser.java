package cz.tacr.elza.service.da;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Closes links of AIPs to the archival description when the package itself takes them away - it was
 * invalidated, it belongs to another fund now, or the part a link pointed at is gone from it.
 *
 * Each link is closed by a change of the unit of description it hangs on, so that the history of
 * the unit records the removal, and the open version of the fund is told about it. The link state
 * of the AIP is left to the caller, which usually changes more than the links.
 */
@Component
public class DaLinkCloser {

    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private EventNotificationService eventNotificationService;

    /**
     * @return number of links closed
     */
    public int close(Collection<ArrDaLink> links) {
        Map<Integer, ArrChange> changeByNode = new HashMap<>();
        for (ArrDaLink link : links) {
            ArrNode node = link.getNode();
            ArrChange change = changeByNode.computeIfAbsent(node.getNodeId(),
                    id -> arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, node));
            link.setDeleteChange(change);
            daLinkRepository.save(link);

            ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFund(node.getFund());
            if (fundVersion != null) {
                eventNotificationService.publishEvent(new EventIdNodeIdInVersion(EventType.DAO_LINK_DELETE,
                        fundVersion.getFundVersionId(), link.getDaoLinkId(),
                        Collections.singletonList(node.getNodeId())));
            }
        }
        return links.size();
    }
}
