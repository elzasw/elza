package cz.tacr.elza.api.interfaces;

import cz.tacr.elza.api.RegScope;

/**
 * Rozhraní pro získání Scope.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
public interface IRegScope<S extends RegScope> {

    S getRegScope();

}
