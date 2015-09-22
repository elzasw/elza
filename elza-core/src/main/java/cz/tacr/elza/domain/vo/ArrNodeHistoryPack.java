package cz.tacr.elza.domain.vo;

import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.ArrFindingAidVersion;


/**
 * Obsahuje seznam změn uzlů a atributů v jednotlivých verzích.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class ArrNodeHistoryPack implements cz.tacr.elza.api.vo.ArrNodeHistoryPack<ArrNodeHistoryItem, ArrFindingAidVersion> {

    private Map<ArrFindingAidVersion, List<ArrNodeHistoryItem>> items;

    @Override
    public Map<ArrFindingAidVersion, List<ArrNodeHistoryItem>> getItems() {
        return items;
    }

    @Override
    public void setItems(final Map<ArrFindingAidVersion, List<ArrNodeHistoryItem>> items) {
        this.items = items;
    }
}
