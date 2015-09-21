package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * TODO
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
