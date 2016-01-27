package cz.tacr.elza.service.eventnotification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventAddNode;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventIdInVersion;
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

    public static EventIdInVersion createIdInVersionEvent(final EventType eventType,
                                                          final ArrFindingAidVersion version,
                                                          final Integer entityId) {
        Assert.notNull(version);

        return new EventIdInVersion(eventType, version.getFindingAidVersionId(), entityId);
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

    public static EventAddNode createAddNodeEvent(final EventType eventType,
                                                  final ArrFindingAidVersion version,
                                                  final ArrLevel staticLevel,
                                                  final ArrLevel addLevel) {
        Assert.notNull(eventType);
        Assert.notNull(version);
        Assert.notNull(staticLevel);
        Assert.notNull(addLevel);

        NodeInfo staticParentNode = staticLevel.getNodeParent() == null ? null
                                                                        : createNodeInfo(staticLevel.getNodeParent());

        return new EventAddNode(eventType, version.getFindingAidVersionId(), createNodeInfo(staticLevel.getNode()),
                staticParentNode, createNodeInfo(addLevel));
    }

    /**
     * Vytvoří nodeinfo.
     *
     * @param level level
     * @return nodeinfo
     */
    private static NodeInfo createNodeInfo(final ArrLevel level) {
        return createNodeInfo(level.getNode());
    }

    private static NodeInfo createNodeInfo(final ArrNode node){
        return new NodeInfo(node.getNodeId(), node.getVersion());
    }

    private static List<NodeInfo> createNodesInfo(final List<ArrLevel> levels) {
        Assert.notNull(levels);
        List<NodeInfo> result = new ArrayList<>(levels.size());
        levels.forEach(l -> result.add(createNodeInfo(l)));

        return result;
    }
}
