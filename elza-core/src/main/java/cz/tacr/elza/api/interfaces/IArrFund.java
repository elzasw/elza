package cz.tacr.elza.api.interfaces;

import cz.tacr.elza.domain.ArrFund;

/**
 * Rozhraní pro získání AS.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
public interface IArrFund {

    /**
     * @return identifikátor archívní pomůcky.
     */
    ArrFund getFund();
}
