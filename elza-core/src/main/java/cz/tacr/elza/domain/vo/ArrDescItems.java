package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Zapouzdření seznamu atributů.
 *
 * @author Martin Šlapa
 * @since 18.9.2015
 */
public class ArrDescItems {

    List<ArrDescItem> descItems;

    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }
}
