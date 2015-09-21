package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Zapouzdření seznamu atributů.
 *
 * @author Martin Šlapa
 * @since 18.9.2015
 */
public class ArrDescItems implements cz.tacr.elza.api.vo.ArrDescItems<ArrDescItem> {

    List<ArrDescItem> descItems;

    @Override
    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    @Override
    public void setDescItems(List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

}
