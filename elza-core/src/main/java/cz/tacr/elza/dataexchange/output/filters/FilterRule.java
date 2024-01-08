package cz.tacr.elza.dataexchange.output.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Result;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;
import liquibase.util.StringUtils;

public class FilterRule {

    public static class When {
        ItemType itemType;

        RulItemSpec itemSpec;

        public ItemType getItemType() {
            return itemType;
        }

        public void setItemType(ItemType itemType) {
            this.itemType = itemType;
        }

        public RulItemSpec getItemSpec() {
            return itemSpec;
        }

        public void setItemSpec(RulItemSpec itemSpec) {
            this.itemSpec = itemSpec;
        }
    }

    public static class AddItem {
        When when;

        ItemType trgItemType;
        RulItemSpec trgItemSpec;

        boolean appendAsNewLine;

        String prefix;

        String value;

        ItemType valueFrom;

        public ItemType getTrgItemType() {
            return trgItemType;
        }

        public void setTrgItemType(ItemType trgItemType) {
            this.trgItemType = trgItemType;
        }

        public RulItemSpec getTrgItemSpec() {
            return trgItemSpec;
        }

        public void setTrgItemSpec(RulItemSpec trgItemSpec) {
            this.trgItemSpec = trgItemSpec;
        }

        public When getWhen() {
            return when;
        }

        public void setWhen(When when) {
            this.when = when;
        }

        public boolean isAppendAsNewLine() {
            return appendAsNewLine;
        }

        public void setAppendAsNewLine(boolean appendAsNewLine) {
            this.appendAsNewLine = appendAsNewLine;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public ItemType getValueFrom() {
            return valueFrom;
        }

        public void setValueFrom(ItemType valueFrom) {
            this.valueFrom = valueFrom;
        }
    };

    When when;

    private boolean hiddenLevel = false;

    private boolean hiddenDao = false;

    private List<ItemType> hiddenItemTypes;

    private List<ReplaceItem> replaceItems;

    private List<AddItem> addItems = Collections.emptyList();

    private List<AddItem> addItemsOnChange = Collections.emptyList();

    public FilterRule(final Def def, final StaticDataProvider sdp) {
        // while
        if (def.getWhen() != null) {
            this.when = new When();
            if (def.getWhen().getItemType() != null) {
                when.setItemType(sdp.getItemTypeByCode(def.getWhen().getItemType()));
                Validate.notNull(when.getItemType(), "Item type not found: %s", def.getWhen().getItemType());
            }
            if (def.getWhen().getItemSpec() != null) {
                when.setItemSpec(sdp.getItemSpecByCode(def.getWhen().getItemSpec()));
                Validate.notNull(when.getItemSpec(), "Item spec not found: %s", def.getWhen().getItemSpec());
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
                    .map(i -> createAddItem(sdp, i))
                    .collect(Collectors.toList());
        }

        if (result.getAddItemsOnChange() != null) {
            addItemsOnChange = result.getAddItemsOnChange().stream()
                    .map(i -> createAddItem(sdp, i))
                    .collect(Collectors.toList());
        }
    }

    private AddItem createAddItem(StaticDataProvider sdp,
                                  cz.tacr.elza.dataexchange.output.filters.FilterConfig.AddItem srcAddItem) {
        AddItem result = new AddItem();
        
        ItemType trgItemType = sdp.getItemTypeByCode(srcAddItem.getItemType());
        Validate.notNull(trgItemType, "Cannot find item type: %s", srcAddItem.getItemType());
        result.setTrgItemType(trgItemType);
        
        if(srcAddItem.getItemSpec()!=null) {
            RulItemSpec trgItemSpec = sdp.getItemSpecByCode(srcAddItem.getItemSpec());
            Validate.notNull(trgItemSpec, "Cannot find item spec: %s", srcAddItem.getItemSpec());
            result.setTrgItemSpec(trgItemSpec);
        }
        
        result.setAppendAsNewLine(srcAddItem.isAppendAsNewLine());
        result.setPrefix(srcAddItem.getPrefix());
        if(srcAddItem.getValue()!=null) {
            result.setValue(srcAddItem.getValue());
        }
        if(srcAddItem.getValueFrom()!=null) {
            ItemType valueFrom = sdp.getItemTypeByCode(srcAddItem.getValueFrom());
            Validate.notNull(trgItemType, "Cannot find item type for valueFrom: %s", srcAddItem.getValueFrom());
            result.setValueFrom(valueFrom);
        }
        
        if(srcAddItem.getWhen()!=null) {
            When when = new When();
            if(srcAddItem.getWhen().getItemType()!=null) {
                ItemType whenItemType = sdp.getItemTypeByCode(srcAddItem.getWhen().getItemType());
                Validate.notNull(whenItemType, "Cannot find item type for when: %s", srcAddItem.getWhen().getItemType());
                when.setItemType(whenItemType);
            }
            if(srcAddItem.getWhen().getItemSpec()!=null) {
                RulItemSpec whenItemSpec = sdp.getItemSpecByCode(srcAddItem.getWhen().getItemSpec());
                Validate.notNull(whenItemSpec, "Cannot find item apwx for when: %s", srcAddItem.getWhen().getItemSpec());
                when.setItemSpec(whenItemSpec);
            }

            result.setWhen(when);
        }
        
        return result;
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

    public List<AddItem> getAddItems() {
        return addItems;
    }

    public List<AddItem> getAddItemsOnChange() {
        return addItemsOnChange;
    }

    private ArrDescItem createDescItemEnum(ItemType itemType, RulItemSpec itemSpec) {
        Validate.isTrue(itemType.getDataType() == DataType.ENUM, "Only ENUMS are supported");
        Validate.notNull(itemSpec, "Specification cannot be empty for ENUM");

        ArrData arrData = new ArrDataNull();
        arrData.setDataType(DataType.ENUM.getEntity());
        arrData.setDataId(-1);

        return createDescItem(itemType, itemSpec, arrData);
    }

    private ArrDescItem createDescItemText(ItemType itemType, RulItemSpec itemSpec, String value) {
        Validate.isTrue(itemType.getDataType() == DataType.TEXT, "Only TEXTs are supported");

        ArrDataText dataText = new ArrDataText();
        dataText.setDataType(DataType.TEXT.getEntity());
        dataText.setDataId(-1);
        dataText.setTextValue(value);

        return createDescItem(itemType, itemSpec, dataText);
    }

    private ArrDescItem createDescItem(ItemType itemType, RulItemSpec itemSpec, ArrData data) {
        ArrDescItem item = new ArrDescItem();
        item.setNode(null);
        item.setItemType(itemType.getEntity());
        item.setItemSpec(itemSpec);
        item.setData(data);
        item.setPosition(0); // need for print
        return item;
    }

    public List<ItemType> getHiddenTypes() {
        return hiddenItemTypes;
    }

    public boolean canApply(Collection<? extends ArrItem> items) {
        return canApply(when, items);
    }

    public static boolean canApply(When when, Collection<? extends ArrItem> items) {
        if (when == null || when.getItemType() == null) {
            return true;
        }
        if (CollectionUtils.isEmpty(items)) {
            return false;
        }
        ItemType itemType = when.getItemType();
        RulItemSpec itemSpec = when.getItemSpec();
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

    /**
     * Add items based on the rule
     * 
     * @param itemsByType
     * @param filter
     * @param changed
     * @param restrItems
     *            Restriction items
     */
    public void addItems(Map<ItemType, List<ArrItem>> itemsByType, ApplyFilter filter, boolean changed,
                         Collection<? extends ArrItem> restrItems) {
        // add itemsOnChange if changed
        if (CollectionUtils.isNotEmpty(addItemsOnChange) && changed) {
            for (AddItem action : addItemsOnChange) {
                addItem(action, itemsByType, filter, restrItems);
            }
        }
        if (CollectionUtils.isNotEmpty(addItems)) {
            for (AddItem action : addItems) {
                addItem(action, itemsByType, filter, restrItems);
            }
        }
    }

    /**
     * Process AddItem action
     * 
     * @param action
     * @param itemsByType
     * @param filter
     * @param restrItems
     */
    private void addItem(AddItem action, Map<ItemType, List<ArrItem>> itemsByType,
                         ApplyFilter filter,
                         Collection<? extends ArrItem> restrItems) {
        if (!canApply(action.getWhen(), restrItems)) {
            return;
        }

        // get source value
        ArrData srcValue = null;
        if (action.getValueFrom() != null) {
            for (ArrItem restrItem : restrItems) {
                if (restrItem.getItemType().getItemTypeId().equals(action.getValueFrom().getItemTypeId())) {
                    srcValue = restrItem.getData();
                    break;
                }
            }
        }

        ArrDescItem descItem = null;
        ArrItem existingItem = filter.getAddedItem(action.getTrgItemType().getEntity(), action.getTrgItemSpec());
        if (action.getTrgItemType().getDataType() == DataType.ENUM) {
            descItem = createDescItemEnum(action.getTrgItemType(), action.getTrgItemSpec());
        } else
        if(action.getTrgItemType().getDataType() == DataType.TEXT) {
            StringBuilder sb = new StringBuilder();
            // append static value
            if (StringUtils.isNotEmpty(action.getValue())) {
                sb.append(action.getValue());
            }
            // append value from other item
            if (srcValue != null) {
                ArrDataText srcDataText = (ArrDataText) srcValue;
                if (StringUtils.isNotEmpty(action.getPrefix())) {
                    sb.append(action.getPrefix());
                }
                sb.append(srcDataText.getTextValue());
            }
            String newText = sb.toString();
            // check if append
            if (existingItem != null && action.isAppendAsNewLine()) {
                if (StringUtils.isNotEmpty(newText)) {
                    // append is possible
                    ArrData data = existingItem.getData();
                    ArrDataText dataText = (ArrDataText) data;

                    // update existing value
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(dataText.getTextValue()).append("\n").append(newText);
                    dataText.setTextValue(sb2.toString());
                }
            } else {
                descItem = createDescItemText(action.getTrgItemType(), action.getTrgItemSpec(), newText);
            }
        }

        if (descItem != null) {
            filter.addItem(descItem);
        }
    }

}
