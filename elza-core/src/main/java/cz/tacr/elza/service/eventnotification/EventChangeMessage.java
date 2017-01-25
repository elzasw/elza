package cz.tacr.elza.service.eventnotification;

import java.util.List;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


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
    private List<AbstractEventSimple> events;

    /**
     * @param events události v aplikaci.
     */
    public EventChangeMessage(final List<AbstractEventSimple> events) {
        this.events = events;
    }

    public List<AbstractEventSimple> getEvents() {
        return events;
    }
}
