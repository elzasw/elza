package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.ItemApInfo;
import cz.tacr.elza.dataexchange.output.writer.PartApInfo;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import org.apache.commons.lang3.Validate;

import java.util.*;

public abstract class ItemDispatcher extends BaseLoadDispatcher<ApItem> {

    private final StaticDataProvider staticData;

    private final Map<Integer, Collection<ApItem>> partItemsMap = new HashMap<>();

    private Collection<ApItem> itemList;

    public ItemDispatcher(StaticDataProvider staticData) {
        this.staticData = staticData;
        itemList = new ArrayList<>();
    }

    @Override
    public void onLoad(ApItem result) {
        RulItemType type = staticData.getItemTypeById(result.getItemTypeId()).getEntity();
        Validate.notNull(type);
        result.setItemType(type);
        if(result.getItemSpecId() != null) {
            RulItemSpec spec = staticData.getItemSpecById(result.getItemSpecId());
            Validate.notNull(spec);
            result.setItemSpec(spec);
        }

        itemList.add(result);

        if(partItemsMap.get(result.getPartId()) != null) {
            partItemsMap.get(result.getPartId()).add(result);
        } else {
            Collection<ApItem> tempList = new ArrayList<>();
            tempList.add(result);
            partItemsMap.put(result.getPartId(), tempList);
        }
    }

    public Map<Integer, Collection<ApItem>> getPartItemsMap() {
        return partItemsMap;
    }

    public Collection<ApItem> getItemList() {
        return itemList;
    }

    /* @Override
    protected void onCompleted() {

    }
*/
}
