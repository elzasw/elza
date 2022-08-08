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
        private When when;
        private Result result;

        public When getWhen() {
            return when;
        }

        public void setWhen(When when) {
            this.when = when;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }
    }

    public static class When {

        protected String itemType;

        protected String itemSpec;

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
