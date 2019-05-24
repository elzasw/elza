package cz.tacr.elza.service;

/**
 * Rozhraní pro vyhodnocení oprávnění pro pořádání podstromu AS.
 */
public interface NodePermissionChecker {

    /**
     * Vyhodnocení oprávnění podle JP.
     *
     * @param nodeId JP pro kterou vyhodnocujeme oprávnění
     * @return true - máme oprávnění pořádat
     */
    boolean checkPermissionInTree(Integer nodeId);

}
