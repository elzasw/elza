package cz.tacr.elza.service;

import java.util.Collection;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;


/**
 * Rozhraní odeslání událostí na klienty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public interface IClientDataChangesService {

    /**
     * Provede okamžité odeslání událostí klientovi.
     *
     * @param events seznam událostí
     */
    void fireEvents(final Collection<AbstractEventSimple> events);

}
