package cz.tacr.elza.dataexchange.output.filters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.CondDef;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Result;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

public class FilterRule {

    public static class Cond {
        ItemType itemType;

        RulItemSpec itemSpec;

        List<Cond> noneOf;
        List<Cond> someOf;

        public Cond(final CondDef condDef, final StaticDataProvider sdp) {
            if (condDef.getItemType() != null) {
                itemType = sdp.getItemTypeByCode(condDef.getItemType());
                Validate.notNull(itemType, "Item type not found: %s", condDef.getItemType());
            }
            if (condDef.getItemSpec() != null) {
                itemSpec = sdp.getItemSpecByCode(condDef.getItemSpec());
                Validate.notNull(itemSpec, "Item spec not found: %s", condDef.getItemSpec());
            }

            if (condDef.getNoneOf() != null) {
                noneOf = condDef.getNoneOf().stream().map(cd -> new Cond(cd, sdp))
                        .collect(Collectors.toList());
            }

            if (condDef.getSomeOf() != null) {
                someOf = condDef.getSomeOf().stream().map(cd -> new Cond(cd, sdp))
                        .collect(Collectors.toList());
            }
        }

        public ItemType getItemType() {
            return itemType;
        }

        public RulItemSpec getItemSpec() {
            return itemSpec;
        }

        public boolean isTrue(Collection<? extends ArrItem> items) {

            if (itemType != null) {
                if (CollectionUtils.isEmpty(items)) {
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
            }

            if (noneOf != null) {
                //
                for (Cond n : noneOf) {
                    if (n.isTrue(items)) {
                        return false;
                    }
                }
                return true;
            }

            if (someOf != null) {
                //
                for (Cond s : someOf) {
                    if (s.isTrue(items)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static class AddItem {
        ItemType trgItemType;
        RulItemSpec trgItemSpec;

        boolean appendAsNewLine;

        String prefix;

        String value;

        ItemType valueFrom;

        ItemType valueFromItem;

        private Integer valueAddYearDefault;
        private ItemType valueAddYearFrom;

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

        public ItemType getValueFromItem() {
            return valueFromItem;
        }

        public void setValueFromItem(ItemType valueFrom) {
            this.valueFromItem = valueFrom;
        }

        public Integer getValueAddYearDefault() {
            return valueAddYearDefault;
        }

        public void setValueAddYearDefault(Integer valueAddYearDefault) {
            this.valueAddYearDefault = valueAddYearDefault;
        }

        public ItemType getValueAddYearFrom() {
            return valueAddYearFrom;
        }

        public void setValueAddYearFrom(ItemType valueAddYearFrom) {
            this.valueAddYearFrom = valueAddYearFrom;
        }

    };

    List<Cond> when = new ArrayList<>();

    private boolean hiddenLevel = false;

    private boolean hiddenDao = false;

    private List<ItemType> hiddenItemTypes;

    private List<ReplaceItem> replaceItems;

    private List<AddItem> addItems = Collections.emptyList();

    private List<AddItem> addItemsOnChange = Collections.emptyList();

    public FilterRule(final Def def, final StaticDataProvider sdp) {
        // while
        if (def.getWhen() != null) {
            for (CondDef condDef : def.getWhen()) {
                Cond cond = new Cond(condDef, sdp);
                when.add(cond);
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
            Validate.notNull(valueFrom, "Cannot find item type for valueFrom: %s", srcAddItem.getValueFrom());
            result.setValueFrom(valueFrom);
        }
        if (srcAddItem.getValueFromItem() != null) {
            ItemType valueFrom = sdp.getItemTypeByCode(srcAddItem.getValueFromItem());
            Validate.notNull(valueFrom, "Cannot find item type for valueFromItem: %s", srcAddItem.getValueFromItem());
            result.setValueFromItem(valueFrom);
        }

        result.setValueAddYearDefault(srcAddItem.getValueAddYearDefault());
        if (srcAddItem.getValueAddYearFrom() != null) {
            ItemType valueFrom = sdp.getItemTypeByCode(srcAddItem.getValueAddYearFrom());
            Validate.notNull(valueFrom, "Cannot find item type for valueAddYearFrom: %s",
                             srcAddItem.getValueAddYearFrom());
            result.setValueAddYearFrom(valueFrom);
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
        if (when != null) {
            for (Cond cond : when) {
                if (!cond.isTrue(items)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Add items based on the rule
     * 
     * @param itemsByType
     * @param filter
     * @param changed
     * @param restrItems
     *            Restriction items
     * @param locale
     */
    public void addItems(Map<ItemType, List<ArrItem>> itemsByType, ApplyFilter filter, boolean changed,
                         Collection<? extends ArrItem> restrItems, Locale locale) {
        // add itemsOnChange if changed
        if (!CollectionUtils.isEmpty(addItemsOnChange) && changed) {
            for (AddItem action : addItemsOnChange) {
                addItem(action, itemsByType, filter, restrItems, locale);
            }
        }
        if (!CollectionUtils.isEmpty(addItems)) {
            for (AddItem action : addItems) {
                addItem(action, itemsByType, filter, restrItems, locale);
            }
        }
    }

    private ArrData getFirstData(Collection<? extends ArrItem> items, ItemType itemType) {
        if (itemType == null || items == null) {
            return null;
        }
        for (ArrItem item : items) {
            if (item.getItemTypeId() != null) {
                if (item.getItemTypeId().equals(itemType.getItemTypeId())) {
                    return item.getData();
                }
            } else if (item.getItemType().getItemTypeId().equals(itemType.getItemTypeId())) {
                return item.getData();
            }
        }
        return null;
    }

    /**
     * Process AddItem action
     * 
     * @param action
     * @param itemsByType
     * @param filter
     * @param restrItems
     * @param locale
     */
    private void addItem(AddItem action, Map<ItemType, List<ArrItem>> itemsByType,
                         ApplyFilter filter,
                         Collection<? extends ArrItem> restrItems, Locale locale) {
        // get source value
        ArrData srcValue = getFirstData(restrItems, action.getValueFrom());

        // get item value
        if(action.getValueFromItem()!=null) {
            List<ArrItem> items = itemsByType.get(action.getValueFromItem());
            if (items != null && items.size() > 0) {
                ArrItem item = items.get(0);
                if (item != null) {
                    srcValue = item.getData();
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
                // add prefix
                if (StringUtils.isNotEmpty(action.getPrefix())) {
                    sb.append(action.getPrefix());
                }
                // add value
                if (srcValue instanceof ArrDataDate) {
                    ArrDataDate srcDataDate = (ArrDataDate) srcValue;
                    LocalDate localDate = srcDataDate.getValue();

                    sb.append(formatLocalDate(action, restrItems, localDate, locale));
                } else if (srcValue instanceof ArrDataUnitdate) {
                    ArrDataUnitdate srcDataUnitdate = (ArrDataUnitdate) srcValue;
                    LocalDateTime localDate = UnitDateConvertor.getLocalDateTimeFromUnitDate(srcDataUnitdate, false);
                                        
                    
                    Integer addYear = null;
                    ArrData srcAddYear = getFirstData(restrItems, action.getValueAddYearFrom());
                    if (srcAddYear != null) {
                        ArrDataInteger di = (ArrDataInteger) srcAddYear;
                        addYear = srcAddYear.getValueInt();
                    }
                    if (addYear == null) {
                        addYear = action.getValueAddYearDefault();
                    }
                    if (addYear != null) {
                        localDate = localDate.plusYears(addYear.longValue());
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                            .withLocale(locale);
                    sb.append(localDate.format(formatter));
                } else if (srcValue instanceof ArrDataText) {
                    ArrDataText srcDataText = (ArrDataText) srcValue;
                    sb.append(srcDataText.getTextValue());
                } else {
                    throw new IllegalStateException("Unsupported type: " + srcValue);
                }

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

    private String formatLocalDate(AddItem action, Collection<? extends ArrItem> restrItems, 
                                   LocalDate localDate, Locale locale) {
        Integer addYear = null;
        ArrData srcAddYear = getFirstData(restrItems, action.getValueAddYearFrom());
        if (srcAddYear != null) {
            ArrDataInteger di = (ArrDataInteger) srcAddYear;
            addYear = srcAddYear.getValueInt();
        }
        if (addYear == null) {
            addYear = action.getValueAddYearDefault();
        }
        if (addYear != null) {
            localDate = localDate.plusYears(addYear.longValue());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(locale);
        return localDate.format(formatter);
    }

}
