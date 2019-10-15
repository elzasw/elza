package cz.tacr.elza.service;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;

public interface ClientEventDispatcher {

    void dispatchEvent(AbstractEventSimple clientEvent);
}
