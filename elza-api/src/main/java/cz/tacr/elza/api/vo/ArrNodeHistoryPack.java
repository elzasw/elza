package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.api.ArrFindingAidVersion;


/**
 * Obsahuje seznam změn uzlů a atributů v jednotlivých verzích.
 *
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public interface ArrNodeHistoryPack<LHI extends ArrNodeHistoryItem, FAV extends ArrFindingAidVersion> extends Serializable {

    Map<FAV, List<LHI>> getItems();


    void setItems(Map<FAV, List<LHI>> items);

}
