package cz.tacr.elza.api.interfaces;

import cz.tacr.elza.api.ArrFund;

/**
 * Rozhraní pro získání AS.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
public interface IArrFund<F extends ArrFund> {

    F getFund();

}
