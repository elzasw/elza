package cz.tacr.elza.dataexchange.output.filters;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Result;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;

public class FilterRule {

    private ItemType itemType;

    private RulItemSpec itemSpec;

    private boolean hiddenLevel = false;

    private boolean hiddenDao = false;

    private List<ItemType> hiddenItemTypes;

    private List<ReplaceItem> replaceItems;

    private List<ArrItem> addItems;

    private List<ArrItem> addItemsOnChange;

    public FilterRule(final Def def, final StaticDataProvider sdp) {
        // while
        if (def.getWhen() != null) {
            if (def.getWhen().getItemType() != null) {
                itemType = sdp.getItemTypeByCode(def.getWhen().getItemType());
                Validate.notNull(itemType, "Item type not found: %s", def.getWhen().getItemType());
            }
            if (def.getWhen().getItemSpec() != null) {
                itemSpec = sdp.getItemSpecByCode(def.getWhen().getItemSpec());
                Validate.notNull(itemSpec, "Item spec not found: %s", def.getWhen().getItemSpec());
            }
        }
        // result
        Result result = def.getResult();

        if (result.getHiddenLevel() != null) {
            hiddenLevel = result.getHiddenLevel();
        }

        if (result.getHiddenDao() != null) {
            hiddenDao = result.getHiddenDao();
        }

        if (result.getHiddenItems() != null) {
            hiddenItemTypes = result.getHiddenItems().stream().map(i -> sdp.getItemTypeByCode(i.getItemType()))
                    .collect(Collectors.toList());
        }

        if (result.getReplaceItems() != null) {
            replaceItems = result.getReplaceItems().stream()
                    .map(i -> new ReplaceItem(sdp.getItemTypeByCode(i.getSource().getItemType()), sdp.getItemTypeByCode(i.getTarget().getItemType())))
                    .collect(Collectors.toList());
        }

        if (result.getAddItems() != null) {
            addItems = result.getAddItems().stream()
                    .map(i -> createDescItem(sdp.getItemTypeByCode(i.getItemType()),
                                             sdp.getItemSpecByCode(i.getItemSpec())))
                    .collect(Collectors.toList());
        }

        if (result.getAddItemsOnChange() != null) {
            addItemsOnChange = result.getAddItemsOnChange().stream()
                    .map(i -> createDescItem(sdp.getItemTypeByCode(i.getItemType()),
                                             sdp.getItemSpecByCode(i.getItemSpec())))
                    .collect(Collectors.toList());
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

    public boolean isHiddenDao() {
        return hiddenDao;
    }

    public List<ReplaceItem> getReplaceItems() {
        return replaceItems;
    }

    public List<ArrItem> getAddItems() {
        return addItems;
    }

    public List<ArrItem> getAddItemsOnChange() {
        return addItemsOnChange;
    }

    private ArrDescItem createDescItem(ItemType itemType, RulItemSpec itemSpec) {
        Validate.isTrue(itemType.getDataType() == DataType.ENUM, "Only ENUMS are supported");

        ArrData arrData = new ArrDataNull();
        arrData.setDataType(DataType.ENUM.getEntity());
        arrData.setDataId(-1);

        ArrDescItem item = new ArrDescItem();
        item.setNode(null);
        item.setItemType(itemType.getEntity());
        item.setItemSpec(itemSpec);
        item.setData(arrData);
        item.setPosition(0); // need for print
        return item;
    }

    public List<ItemType> getHiddenTypes() {
        return hiddenItemTypes;
    }

    public boolean canApply(Collection<? extends ArrItem> items) {
        if (itemType != null) {
            if (items == null) {
                return false;
            }
            for (ArrItem soiItem : items) {
                if (itemType.getItemTypeId().equals(soiItem.getItemTypeId())) {
                    if (itemSpec != null) {
                        if (!itemSpec.getItemSpecId().equals(soiItem.getItemSpecId())) {
                            continue;
                        } else {
                            return true;
                        }
                    } else {
                        // item spec is null compare only by itemType
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

}
