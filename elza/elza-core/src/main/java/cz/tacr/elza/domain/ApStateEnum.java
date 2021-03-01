package cz.tacr.elza.domain;

import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

/**
 * Stav entity.
 *
 * Používané pro entity:
 *  - {@link ApAccessPoint}
 *  - {@link ApPart}
 */
public enum ApStateEnum implements AccessPointCacheSerializable {

    /**
     * V pořádku, platné.
     */
    OK,

    /**
     * Chyba při generování.
     */
    ERROR,

    /**
     * Připraveno k přegenerování.
     */
    INIT

}
