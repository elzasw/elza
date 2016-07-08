package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Obsahuje seznam změn uzlů a atributů v jednotlivých verzích.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public interface ArrNodeHistoryPack<LHI extends ArrNodeHistoryItem> extends Serializable {

    Map<Integer, List<LHI>> getItems();


    void setItems(Map<Integer, List<LHI>> items);

}
