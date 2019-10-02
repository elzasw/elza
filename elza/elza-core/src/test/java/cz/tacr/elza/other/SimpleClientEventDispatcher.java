package cz.tacr.elza.other;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * Testovací odesílač událostí.
 */
public class SimpleClientEventDispatcher implements ClientEventDispatcher {

    private List<AbstractEventSimple> firedEvents = new ArrayList<>();

    @Override
    public void dispatchEvent(AbstractEventSimple clientEvent) {
        firedEvents.add(clientEvent);
    }

    public Collection<AbstractEventSimple> getFiredEvents() {
        return Collections.unmodifiableCollection(firedEvents);
    }

    public void clearFiredEvents() {
        firedEvents.clear();
    }
}
