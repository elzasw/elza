package cz.tacr.elza.senditemtoelzarequest.service;

import cz.tacr.elza.ws.WsClient;
import org.dspace.content.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Servisní třída pro odesílání požadavků do systému Elza
 * Created by Marbes Consulting
 * tomas.havranek@marbes.cz / 04.07.2019.
 */
@Service
public class ElzaRequestService {
    @Autowired
    private WsClient wsClient;

    public void sendItemToElza(Item item) {
        wsClient.sendItemToElza(item);
    }
}
