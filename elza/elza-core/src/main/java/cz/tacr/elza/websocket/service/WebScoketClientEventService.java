package cz.tacr.elza.websocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.service.ClientEventDispatcher;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;

/**
 * Service is defined by {@link ElzaCore#clientEventDispatcher()}
 */
public class WebScoketClientEventService implements ClientEventDispatcher {

    public static final String API_CHANGES_DESTINATION = "/topic/api/changes";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void dispatchEvent(AbstractEventSimple clientEvent) {
        messagingTemplate.convertAndSend(API_CHANGES_DESTINATION, clientEvent);
    }
}
