package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrDescItem;


/**
 * Zapouzdření seznamu atributů {@link ArrDescItem}.
 *
 * @param <DI> {@link ArrDescItem}
 * @author Martin Šlapa
 * @since 18.9.2015
 */
public interface ArrDescItems<DI extends ArrDescItem> extends Serializable {

    List<DI> getDescItems();


    void setDescItems(List<DI> descItems);

}
