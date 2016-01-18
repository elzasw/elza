package cz.tacr.elza.service.eventnotification.events;

import java.util.Set;


/**
 * Rozhraní pro události, které změnili data stromu verze a je potřeba tento strom přenačíst v klientovi.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.01.2016
 */
public interface VersionTreeChange {

    /**
     * Seznam změněných verzí.
     *
     * @return množina id verzí
     */
    Set<Integer> getChangedVersionIds();
}
