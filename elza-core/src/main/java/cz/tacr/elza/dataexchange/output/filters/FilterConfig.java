package cz.tacr.elza.dataexchange.output.filters;

import java.util.List;

public class FilterConfig {

    protected List<String> restrictions;

    protected List<Def> defs;

    public List<String> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }

    public List<Def> getDefs() {
        return defs;
    }

    public void setDefs(List<Def> defs) {
        this.defs = defs;
    }

    public static class Def {
        /**
         * List of conditions
         * 
         * All have to match
         */
        private List<CondDef> when;
        private Result result;

        public List<CondDef> getWhen() {
            return when;
        }

        public void setWhen(List<CondDef> when) {
            this.when = when;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }
    }

    public static class CondDef {

        protected String itemType;

        protected String itemSpec;

        protected List<CondDef> noneOf;
        protected List<CondDef> someOf;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public String getItemSpec() {
            return itemSpec;
        }

        public void setItemSpec(String itemSpec) {
            this.itemSpec = itemSpec;
        }

        public List<CondDef> getNoneOf() {
            return noneOf;
        }

        public void setNoneOf(List<CondDef> noneOf) {
            this.noneOf = noneOf;
        }

        public List<CondDef> getSomeOf() {
            return someOf;
        }

        public void setSomeOf(List<CondDef> someOf) {
            this.someOf = someOf;
        }
    }

    public static class Result {

        protected Boolean hiddenLevel;

        protected Boolean hiddenDao;

        protected List<ItemTypeCode> hiddenItems;

        protected List<ReplaceItemCode> replaceItems;

        protected List<AddItem> addItems;

        protected List<AddItem> addItemsOnChange;

        public Boolean getHiddenLevel() {
            return hiddenLevel;
        }

        public void setHiddenLevel(Boolean hiddenLevel) {
            this.hiddenLevel = hiddenLevel;
        }

        public Boolean getHiddenDao() {
            return hiddenDao;
        }

        public void setHiddenDao(Boolean hiddenDao) {
            this.hiddenDao = hiddenDao;
        }

        public List<ItemTypeCode> getHiddenItems() {
            return hiddenItems;
        }

        public void setHiddenItems(List<ItemTypeCode> hiddenItems) {
            this.hiddenItems = hiddenItems;
        }

        public List<ReplaceItemCode> getReplaceItems() {
            return replaceItems;
        }

        public void setReplaceItems(List<ReplaceItemCode> replaceItems) {
            this.replaceItems = replaceItems;
        }

        public List<AddItem> getAddItems() {
            return addItems;
        }

        public void setAddItems(List<AddItem> addItems) {
            this.addItems = addItems;
        }

        public List<AddItem> getAddItemsOnChange() {
            return addItemsOnChange;
        }

        public void setAddItemsOnChange(List<AddItem> addItemsOnChange) {
            this.addItemsOnChange = addItemsOnChange;
        }
    }

    public static class ReplaceItemCode {
        protected ItemTypeCode source;
        protected ItemTypeCode target;

        public ItemTypeCode getSource() {
            return source;
        }

        public void setSource(ItemTypeCode source) {
            this.source = source;
        }

        public ItemTypeCode getTarget() {
            return target;
        }

        public void setTarget(ItemTypeCode target) {
            this.target = target;
        }
    }

    public static class AddItem {

        private String itemType;

        private String itemSpec;

        /**
         * Flag to append value to existing item.
         * Value will be appended on new line.
         */
        private boolean appendAsNewLine = false;

        /**
         * Value of the item.
         * 
         * Can be used for text and string data types.
         */
        private String value;

        /**
         * Optional prefix for value.
         * 
         * Used with valueFrom.
         */
        private String prefix;

        /**
         * Item type of value source item (from soi)
         */
        private String valueFrom;

        /**
         * Item type of value source item (from descr items)
         */
        private String valueFromItem;

        /**
         * Add number of years (default value)
         */
        private Integer valueAddYearDefault;

        /**
         * Add number of years (item type)
         * 
         * Default value is used if not defined
         */
        private String valueAddYearFrom;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public String getItemSpec() {
            return itemSpec;
        }

        public void setItemSpec(String itemSpec) {
            this.itemSpec = itemSpec;
        }

        public boolean isAppendAsNewLine() {
            return appendAsNewLine;
        }

        public void setAppendAsNewLine(boolean appendAsNewLine) {
            this.appendAsNewLine = appendAsNewLine;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getValueFrom() {
            return valueFrom;
        }

        public void setValueFrom(String valueFrom) {
            this.valueFrom = valueFrom;
        }

        public String getValueFromItem() {
            return valueFromItem;
        }        

        public void setValueFromItem(String valueFromItem) {
            this.valueFromItem = valueFromItem;
        }

        public Integer getValueAddYearDefault() {
            return valueAddYearDefault;
        }

        public void setValueAddYearDefault(Integer valueAddYear) {
            this.valueAddYearDefault = valueAddYear;
        }

        public String getValueAddYearFrom() {
            return valueAddYearFrom;
        }

        public void setValueAddYearFrom(String valueAddYearFrom) {
            this.valueAddYearFrom = valueAddYearFrom;
        }

    }

    public static class ItemTypeCode {
        protected String itemType;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }
    }
}
