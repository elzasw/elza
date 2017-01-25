package cz.tacr.elza.domain.vo;

import java.util.List;
import java.util.Map;


/**
 * Obsahuje seznam změn uzlů a atributů v jednotlivých verzích.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class ArrNodeHistoryPack {

    private Map<Integer, List<ArrNodeHistoryItem>> items;

    public Map<Integer, List<ArrNodeHistoryItem>> getItems() {
        return items;
    }

    public void setItems(final Map<Integer, List<ArrNodeHistoryItem>> items) {
        this.items = items;
    }
}
