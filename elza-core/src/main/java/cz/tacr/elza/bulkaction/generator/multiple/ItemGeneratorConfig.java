package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class ItemGeneratorConfig
    implements ActionConfig
{
    
    public static class StructuredObjectItemConfig {
        String itemType;
        String valueFrom;
        
        /**
         * Prvek popisu pro uvedení počáteční hodnoty
         * Uplatní se v případě počtu většího než 1
         */
        String startItemType;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public String getValueFrom() {
            return valueFrom;
        }

        public void setValueFrom(String valueFrom) {
            this.valueFrom = valueFrom;
        }

        public String getStartItemType() {
            return startItemType;
        }

        public void setStartItemType(String startItemType) {
            this.startItemType = startItemType;
        }        
    }

    public static class StructuredObjectConfig {
        StructuredObjectItemConfig prefix;
        StructuredObjectItemConfig mainValue;
        
        public StructuredObjectItemConfig getPrefix() {
            return prefix;
        }
        public void setPrefix(StructuredObjectItemConfig prefix) {
            this.prefix = prefix;
        }
        public StructuredObjectItemConfig getMainValue() {
            return mainValue;
        }
        public void setMainValue(StructuredObjectItemConfig mainValue) {
            this.mainValue = mainValue;
        }
    }
    
    public static class CreateItem {
        String itemType;
        StructuredObjectConfig structuredObject;
        public String getItemType() {
            return itemType;
        }
        public void setItemType(String itemType) {
            this.itemType = itemType;
        }
        public StructuredObjectConfig getStructuredObject() {
            return structuredObject;
        }
        public void setStructuredObject(StructuredObjectConfig structuredObject) {
            this.structuredObject = structuredObject;
        }
        
        
    }
    
    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

    WhenConditionConfig when;
    
    List<CreateItem> create;

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

    public WhenConditionConfig getWhen() {
        return when;
    }

    public void setWhen(WhenConditionConfig when) {
        this.when = when;
    }

    public List<CreateItem> getCreate() {
        return create;
    }

    public void setCreate(List<CreateItem> createItemList) {
        this.create = createItemList;
    }

    @Override
    public Class<? extends Action> getActionClass() {
        return ItemGeneratorAction.class;
    }
}
