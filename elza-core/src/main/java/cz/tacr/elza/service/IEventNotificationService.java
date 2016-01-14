package cz.tacr.elza.service;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * Servisní třída pro registraci události, která bude odeslána klientům.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public interface IEventNotificationService {


    /**
     * Provede uložení události a pozdější odeslání klientům.
     *
     * @param event událost
     */
    void publishEvent(AbstractEventSimple event);


}
