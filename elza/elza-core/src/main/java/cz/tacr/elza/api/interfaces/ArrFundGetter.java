package cz.tacr.elza.api.interfaces;

import cz.tacr.elza.domain.ArrFund;

/**
 * Rozhraní pro získání AS.
 *
 * @since 27.04.2016
 */
public interface ArrFundGetter {

    /**
     * @return identifikátor archívní pomůcky.
     */
    ArrFund getFund();
}
