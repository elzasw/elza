package cz.tacr.elza.dataexchange.output.filters;

import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig.Def;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig.Result;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;

public class FilterRule {

    final private ItemType itemType;

    final private RulItemSpec itemSpec;

    final private boolean hiddenLevel;

    final private List<Integer> hiddenItemTypeIds;

    final private List<ReplaceItem> replaceItems;

    final private List<ArrItem> addedArrItem;
    
    public FilterRule(final Def def, final StaticDataProvider sdp) {
        // while
        itemType = sdp.getItemTypeByCode(def.getWhen().getItemType());
        itemSpec = sdp.getItemSpecByCode(def.getWhen().getItemSpec());
        // result
        Result result = def.getResult();
        hiddenLevel = result.getHiddenLevel();
        if (result.getHiddenItems() != null) {
            hiddenItemTypeIds = result.getHiddenItems().stream().map(i -> sdp.getItemTypeByCode(i.getItemType()).getItemTypeId()).collect(Collectors.toList());
        } else {
            hiddenItemTypeIds = null;
        }
        if (result.getReplaceItems() != null) {
            replaceItems = result.getReplaceItems().stream()
                    .map(i -> new ReplaceItem(sdp.getItemTypeByCode(i.getSource().getItemType()), sdp.getItemTypeByCode(i.getTarget().getItemType())))
                    .collect(Collectors.toList());
        } else {
            replaceItems = null;
        }
        if (result.getAddItems() != null) {
            addedArrItem = result.getAddItems().stream()
                    .map(i -> createArrDescItem(sdp.getItemTypeByCode(i.getItemType()), sdp.getItemSpecByCode(i.getItemSpec())))
                    .collect(Collectors.toList());
        } else {
            addedArrItem = null;
        }
    }

    public ItemType getItemType() {
        return itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public boolean isHiddenLevel() {
        return hiddenLevel;
    }

    public List<Integer> getHiddenItemTypeIds() {
        return hiddenItemTypeIds;
    }

    public List<ReplaceItem> getReplaceItems() {
        return replaceItems;
    }

    public List<ArrItem> getAddedArrItem() {
        return addedArrItem;
    }

    private ArrItem createArrDescItem(ItemType itemType, RulItemSpec itemSpec) {
        ArrData arrData = new ArrDataNull();
        arrData.setDataType(DataType.ENUM.getEntity());
        arrData.setDataId(-1);

        ArrDescItem addedArrItem = new ArrDescItem();
        addedArrItem.setNode(null);
        addedArrItem.setItemType(itemType.getEntity());
        addedArrItem.setItemSpec(itemSpec);
        addedArrItem.setData(arrData);
        return addedArrItem;
    }
}
