package cz.tacr.elza.dataexchange.output.filters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.CondDef;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Result;
import cz.tacr.elza.dataexchange.output.filters.FilterRule.AddItem.AddItemType;
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
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import liquibase.util.StringUtils;

public class FilterRule {

    @FunctionalInterface
    public interface ValueComparator {
        public boolean isTrue(FilterRuleContext filterRuleContext, ArrData data);
    }

    public static class Cond {
        ItemType itemType;

        RulItemSpec itemSpec;

        List<Cond> noneOf;
        List<Cond> someOf;

        ValueComparator valueComparator;

        public Cond(final CondDef condDef, final StaticDataProvider sdp) {
            if (condDef.getItemType() != null) {
                itemType = sdp.getItemTypeByCode(condDef.getItemType());
                Validate.notNull(itemType, "Item type not found: %s", condDef.getItemType());
            }
            if (condDef.getItemSpec() != null) {
                itemSpec = sdp.getItemSpecByCode(condDef.getItemSpec());
                Validate.notNull(itemSpec, "Item spec not found: %s", condDef.getItemSpec());
            }
            if (condDef.getLower() != null) {
                if (itemType.getDataType() == DataType.DATE) {
                    if ("now".equals(condDef.getLower())) {
                        // compare date
                        valueComparator = (frCtx, data) -> {
                            if (data != null && data instanceof ArrDataDate) {
                                ArrDataDate dd = (ArrDataDate) data;
                                LocalDate now = LocalDate.now();
                                return dd.getValue().isBefore(now);
                            }
                            return false;
                        };
                    } else {
                        throw new BusinessException("Unsupported date value comparison, lower: " + condDef.getLower(),
                                BaseCode.INVALID_STATE);
                    }
                } else {
                    throw new BusinessException("Unsupported value comparison", BaseCode.INVALID_STATE);
                }
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

        public boolean isTrue(FilterRuleContext filterRuleContext) {

            if (itemType != null) {
                ArrData data = filterRuleContext.getFirstData(itemType, itemSpec);
                if (data != null) {
                    if (valueComparator != null) {
                        return valueComparator.isTrue(filterRuleContext, data);
                    } else {
                        return true;
                    }
                }
                return false;
            }

            if (noneOf != null) {
                //
                for (Cond n : noneOf) {
                    if (n.isTrue(filterRuleContext)) {
                        return false;
                    }
                }
                return true;
            }

            if (someOf != null) {
                //
                for (Cond s : someOf) {
                    if (s.isTrue(filterRuleContext)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static class AddItem {
        enum AddItemType {
            /**
             * Standard item. e.g. DescItem or OutputItem
             */
            STANDARD_ITEM,
            /**
             * Virtual restriction item
             */
            VIRTUAL_RESTR_ITEM
        };

        ItemType trgItemType;
        RulItemSpec trgItemSpec;

        boolean appendAsNewLine;
        private boolean updateWithLower;

        String prefix;

        String value;

        ItemType valueFrom;

        ItemType valueFromItem;

        private Integer valueAddYearDefault;
        private ItemType valueAddYearFrom;
        private final AddItemType addItemType;

        public AddItem(final AddItemType addItemType) {
            this.addItemType = addItemType;
        }

        /**
         * Return type of addition
         */
        public AddItemType getAddType() {
            return addItemType;
        }

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

        public boolean isUpdateWithLower() {
            return updateWithLower;
        }

        public void setUpdateWithLower(final boolean updateWithLower) {
            this.updateWithLower = updateWithLower;

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

    private boolean breakEval = false;

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

        if (result.getBreakEval() != null && result.getBreakEval().booleanValue()) {
            breakEval = true;
        }
    }

    private AddItem createAddItem(StaticDataProvider sdp,
                                  cz.tacr.elza.dataexchange.output.filters.FilterConfig.AddItem srcAddItem) {
        
        AddItemType addItemType = AddItemType.STANDARD_ITEM;
        if (srcAddItem.isRestrictionItem()) {
            addItemType = AddItemType.VIRTUAL_RESTR_ITEM;
        }
        AddItem result = new AddItem(addItemType);

        ItemType trgItemType = sdp.getItemTypeByCode(srcAddItem.getItemType());
        Validate.notNull(trgItemType, "Cannot find item type: %s", srcAddItem.getItemType());
        result.setTrgItemType(trgItemType);
        
        if(srcAddItem.getItemSpec()!=null) {
            RulItemSpec trgItemSpec = sdp.getItemSpecByCode(srcAddItem.getItemSpec());
            Validate.notNull(trgItemSpec, "Cannot find item spec: %s", srcAddItem.getItemSpec());
            result.setTrgItemSpec(trgItemSpec);
        }
        
        result.setAppendAsNewLine(srcAddItem.isAppendAsNewLine());
        result.setUpdateWithLower(srcAddItem.isUpdateWithLower());
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

    public boolean isBreakEval() {
        return breakEval;
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

        ArrData data = new ArrDataNull();
        data.setDataType(DataType.ENUM.getEntity());
        data.setDataId(-1);

        return createDescItem(itemType, itemSpec, data);
    }

    private ArrDescItem createDescItemDate(ItemType itemType, RulItemSpec itemSpec,
                                           LocalDate value) {
        Validate.isTrue(itemType.getDataType() == DataType.DATE, "Only DATEs are supported");

        ArrDataDate data = new ArrDataDate();
        data.setDataType(DataType.DATE.getEntity());
        data.setDataId(-1);
        data.setValue(value);

        return createDescItem(itemType, itemSpec, data);
    }

    private ArrDescItem createDescItemText(ItemType itemType, RulItemSpec itemSpec, String value) {
        Validate.isTrue(itemType.getDataType() == DataType.TEXT, "Only TEXTs are supported");

        ArrDataText data = new ArrDataText();
        data.setDataType(DataType.TEXT.getEntity());
        data.setDataId(-1);
        data.setTextValue(value);

        return createDescItem(itemType, itemSpec, data);
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

    public boolean canApply(FilterRuleContext filterRuleContext) {
        if (when != null) {
            for (Cond cond : when) {
                if (!cond.isTrue(filterRuleContext)) {
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
                         FilterRuleContext filterRuleContext, Locale locale) {
        // add itemsOnChange if changed
        if (CollectionUtils.isNotEmpty(addItemsOnChange) && changed) {
            for (AddItem action : addItemsOnChange) {
                addItem(action, itemsByType, filter, filterRuleContext, locale);
            }
        }
        if (CollectionUtils.isNotEmpty(addItems)) {
            for (AddItem action : addItems) {
                addItem(action, itemsByType, filter, filterRuleContext, locale);
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
     * @param locale
     */
    private void addItem(AddItem action, Map<ItemType, List<ArrItem>> itemsByType,
                         ApplyFilter filter,
                         FilterRuleContext filterRuleContext, Locale locale) {
        // get source value
        ArrData srcValue = filterRuleContext.getFirstData(action.getValueFrom(), null);

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
        if (action.getTrgItemType().getDataType() == DataType.DATE) {
            LocalDate localDate = LocalDate.now();
            if (srcValue != null) {
                if (srcValue instanceof ArrDataDate) {
                    ArrDataDate srcDataDate = (ArrDataDate) srcValue;
                    localDate = srcDataDate.getValue();
                } else if (srcValue instanceof ArrDataUnitdate) {
                    ArrDataUnitdate srcDataUnitdate = (ArrDataUnitdate) srcValue;
                    LocalDateTime localDateTime = UnitDateConvertor.getLocalDateTimeFromUnitDate(srcDataUnitdate,
                                                                                                 false);
                    localDate = localDateTime.toLocalDate();
                } else {
                    throw new IllegalStateException("Unsupported type: " + srcValue);
                }
            }
            localDate = prepareLocalDate(action, filterRuleContext, localDate);

            // check if update existing
            if (existingItem != null && action.isUpdateWithLower()) {
                ArrDataDate prevValue = ((ArrDataDate) existingItem.getData());
                if (prevValue.getValue().isAfter(localDate)) {
                    prevValue.setValue(localDate);
                }
            } else {
                descItem = createDescItemDate(action.getTrgItemType(), action.getTrgItemSpec(), localDate);
            }
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

                    sb.append(formatLocalDate(action, filterRuleContext, localDate, locale));
                } else if (srcValue instanceof ArrDataUnitdate) {
                    ArrDataUnitdate srcDataUnitdate = (ArrDataUnitdate) srcValue;
                    LocalDateTime localDateTime = UnitDateConvertor.getLocalDateTimeFromUnitDate(srcDataUnitdate,
                                                                                                 false);
                    LocalDate localDate = localDateTime.toLocalDate();
                    localDate = prepareLocalDate(action, filterRuleContext, localDate);
                    
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
        } else {
            throw new IllegalStateException("Unsupported type: " + action.getTrgItemType().getDataType());
        }

        if (descItem != null) {
            switch (action.getAddType()) {
            case STANDARD_ITEM:
                filter.addItem(descItem);
                break;
            case VIRTUAL_RESTR_ITEM:
                filterRuleContext.addRestrItem(descItem);
                break;
            }
        }
    }

    /**
     * Prepare localdate as specified in action
     * 
     * @param action
     * @param localDate
     * @return
     */
    private LocalDate prepareLocalDate(AddItem action,
                                       FilterRuleContext filterRuleContext,
                                       LocalDate localDate) {
        Integer addYear = null;
        ArrData srcAddYear = filterRuleContext.getFirstData(action.getValueAddYearFrom(), null);
        if (srcAddYear != null) {
            ArrDataInteger di = (ArrDataInteger) srcAddYear;
            addYear = di.getIntegerValue();
        }
        if (addYear == null) {
            addYear = action.getValueAddYearDefault();
        }
        if (addYear != null) {
            localDate = localDate.plusYears(addYear.longValue());
        }
        return localDate;
    }

    private String formatLocalDate(AddItem action,
                                   FilterRuleContext filterRuleContext,
                                   LocalDate localDate, Locale locale) {
        localDate = prepareLocalDate(action, filterRuleContext, localDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(locale);
        return localDate.format(formatter);
    }

}
