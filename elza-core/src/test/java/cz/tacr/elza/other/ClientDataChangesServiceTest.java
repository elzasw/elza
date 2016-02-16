package cz.tacr.elza.other;

import java.util.Collection;

import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * Testovací odesílač událostí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class ClientDataChangesServiceTest implements IClientDataChangesService {

    private Collection<AbstractEventSimple> lastFiredEvents;


    @Override
    public void fireEvents(final Collection<AbstractEventSimple> events) {
        lastFiredEvents = events;
    }

    public Collection<AbstractEventSimple> getLastFiredEvents() {
        return lastFiredEvents;
    }
}
