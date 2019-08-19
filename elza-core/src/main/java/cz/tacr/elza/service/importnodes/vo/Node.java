package cz.tacr.elza.service.importnodes.vo;

import cz.tacr.elza.service.importnodes.vo.descitems.Item;

import java.util.Collection;

/**
 * Rozhraní pro reprezentaci uzlu ve stromu.
 *
 * @since 19.07.2017
 */
public interface Node {

    /**
     * @return jednoznačný identifikátor uzlu
     */
    String getUuid();

    Collection<? extends Item> getItems();

}
