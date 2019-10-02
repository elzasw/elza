package cz.tacr.elza.service.importnodes.vo.descitems;

import java.math.BigDecimal;

/**
 * Rozhraní pro reprezentaci atributu.
 *
 * @since 19.07.2017
 */
public interface ItemDecimal extends Item {

    BigDecimal getValue();

}
