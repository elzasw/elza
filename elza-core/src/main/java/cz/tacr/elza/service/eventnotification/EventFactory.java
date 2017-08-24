package cz.tacr.elza.service.eventnotification;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.eventnotification.events.*;
import cz.tacr.elza.service.eventnotification.events.vo.NodeInfo;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;


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

    public static EventIdsInVersion createIdsInVersionEvent(final EventType eventType,
                                                            final ArrFundVersion version,
                                                            final Integer... entityIds) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        return new EventIdsInVersion(eventType, version.getFundVersionId(), entityIds);
    }

    public static EventIdInVersion createIdInVersionEvent(final EventType eventType,
                                                          final ArrFundVersion version,
                                                          final Integer entityId) {
        Assert.notNull(version, "Verze AS musí být vyplněna");

        return new EventIdInVersion(eventType, version.getFundVersionId(), entityId);
    }

    public static EventStringInVersion createStringInVersionEvent(final EventType eventType,
                                                                  final Integer versionId,
                                                                  final String entityString) {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");

        return new EventStringInVersion(eventType, versionId, entityString);
    }

    public static EventIdAndStringInVersion createStringAndIdInVersionEvent(final EventType eventType,
                                                                            final Integer versionId,
                                                                            final Integer entityId,
                                                                            final String entityString) {
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");

        return new EventIdAndStringInVersion(eventType, versionId, entityId, entityString);
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
                                                final ArrFundVersion version) {
        return new EventNodeMove(eventType, version.getFundVersionId(), createNodeInfo(staticLevel),
                createNodesInfo(transportLevels));
    }

    public static EventAddNode createAddNodeEvent(final EventType eventType,
                                                  final ArrFundVersion version,
                                                  final ArrLevel staticLevel,
                                                  final ArrLevel addLevel) {
        Assert.notNull(eventType, "Typ eventu musí být vyplněn");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(staticLevel, "Referenční level musí být vyplněn");
        Assert.notNull(addLevel, "Level musí být vyplněn");

        NodeInfo staticParentNode = staticLevel.getNodeParent() == null ? null
                                                                        : createNodeInfo(staticLevel.getNodeParent());

        return new EventAddNode(eventType, version.getFundVersionId(), createNodeInfo(staticLevel.getNode()),
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
        Assert.notNull(levels, "Levely musí být vyplněny");
        List<NodeInfo> result = new ArrayList<>(levels.size());
        levels.forEach(l -> result.add(createNodeInfo(l)));

        return result;
    }
}
