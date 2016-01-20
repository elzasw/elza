package cz.tacr.elza.service.eventnotification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventNodeMove;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.vo.NodeInfo;


/**
 * Továrna na události.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class EventFactory {

    /**
     * Vytvoří událost obsahující jako data id.
     *
     * @param eventType typ události.
     * @param ids       seznam id
     * @return událost
     */
    public static EventId createIdEvent(final EventType eventType, final Integer... ids) {
        return new EventId(eventType, ids);
    }

    /**
     * Vytvoří událost přesunutí uzlu.
     *
     * @param eventType       typ přesunu
     * @param staticLevel     statický uzel (před/za/pod který přesouváme)
     * @param transportLevels seznam přesouvaných uzlů
     * @param version         verze stromu
     * @return událost
     */
    public static EventNodeMove createMoveEvent(final EventType eventType,
                                                final ArrLevel staticLevel,
                                                final List<ArrLevel> transportLevels,
                                                final ArrFindingAidVersion version) {
        return new EventNodeMove(eventType, version.getFindingAidVersionId(), createNodeInfo(staticLevel),
                createNodesInfo(transportLevels));
    }

    /**
     * Vytvoří nodeinfo.
     *
     * @param level level
     * @return nodeinfo
     */
    private static NodeInfo createNodeInfo(final ArrLevel level) {
        return new NodeInfo(level.getNode().getNodeId(), level.getNode().getVersion());
    }

    private static List<NodeInfo> createNodesInfo(final List<ArrLevel> levels) {
        Assert.notNull(levels);
        List<NodeInfo> result = new ArrayList<>(levels.size());
        levels.forEach(l -> result.add(createNodeInfo(l)));

        return result;
    }
}
