package cz.tacr.elza.domain.vo;

import java.util.List;
import java.util.Map;


/**
 * Obsahuje seznam změn uzlů a atributů v jednotlivých verzích.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class ArrNodeHistoryPack implements cz.tacr.elza.api.vo.ArrNodeHistoryPack<ArrNodeHistoryItem> {

    private Map<Integer, List<ArrNodeHistoryItem>> items;

    @Override
    public Map<Integer, List<ArrNodeHistoryItem>> getItems() {
        return items;
    }

    @Override
    public void setItems(final Map<Integer, List<ArrNodeHistoryItem>> items) {
        this.items = items;
    }
}
