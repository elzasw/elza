package cz.tacr.elza.service.eventnotification;

import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;


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

}
