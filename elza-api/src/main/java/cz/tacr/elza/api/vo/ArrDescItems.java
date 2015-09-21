package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrDescItem;


/**
 * TODO
 */
public interface ArrDescItems <DI extends ArrDescItem> extends Serializable {

    List<DI> getDescItems();

    void setDescItems(List<DI> descItems);

}
