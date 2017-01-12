package cz.tacr.elza.service.websocket;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import cz.tacr.elza.service.IClientDataChangesService;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class ClientDataChangesService implements IClientDataChangesService {

    private static final String API_CHANGES_DESTINATION = "/topic/api/changes";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @Override
	public void fireEvents(final Collection<AbstractEventSimple> events) {
        messagingTemplate
                .convertAndSend(API_CHANGES_DESTINATION, new WebsocketDataVO(WebsocketDataType.EVENT, events));
    }

}
