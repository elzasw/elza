package cz.tacr.elza.service.eventnotification;

import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * Událost odesílaná do kontextu aplikace s události vyvolanými v transakci.
 *
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
        Validate.notNull(events, "Missing list of events");
        this.events = events;
    }

    /**
     * Return list of events
     * 
     * Return alway non null object
     * 
     * @return List of events
     */
    public List<AbstractEventSimple> getEvents() {
        return events;
    }
}
