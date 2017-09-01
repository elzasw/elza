package cz.tacr.elza.service.importnodes.vo;

/**
 * Rozhraní pro lambda volání změnu hloubky ve stromu.
 *
 * @since 19.07.2017
 */
@FunctionalInterface
public interface DeepCallback {

    /**
     * @param changeDeep zavolání průchodu stromem
     */
    void call(ChangeDeep changeDeep);
}
