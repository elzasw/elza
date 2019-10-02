package cz.tacr.elza.service.importnodes.vo.descitems;

import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Rozhraní pro reprezentaci atributu.
 *
 * @since 19.07.2017
 */
public interface ItemJsonTable extends Item {

    ElzaTable getValue();

}
