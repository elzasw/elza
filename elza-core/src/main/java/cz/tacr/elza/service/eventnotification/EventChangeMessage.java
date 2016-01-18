package cz.tacr.elza.service.eventnotification;

import java.util.Map;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Událost odesílaná do kontextu aplikace s události vyvolanými v transakci.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.01.2016
 */
public class EventChangeMessage {

    /**
     * Mapa událostí v aplikaci.
     */
    private Map<EventType, AbstractEventSimple> changeMap;

    /**
     * @param changeMap Mapa událostí v aplikaci.
     */
    public EventChangeMessage(final Map<EventType, AbstractEventSimple> changeMap) {
        this.changeMap = changeMap;
    }

    public Map<EventType, AbstractEventSimple> getChangeMap() {
        return changeMap;
    }
}
