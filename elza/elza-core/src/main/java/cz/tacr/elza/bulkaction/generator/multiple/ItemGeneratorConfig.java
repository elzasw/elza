package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;
import java.util.Map;

public class ItemGeneratorConfig
    implements ActionConfig
{
    
    public static class StructuredObjectItemConfig {
        String itemType;
        String valueFrom;
        Map<String, String> valueSpecMapping;

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

        public Map<String, String> getValueSpecMapping() {
            return valueSpecMapping;
        }

        public void setValueSpecMapping(Map<String, String> valueSpecMapping) {
            this.valueSpecMapping = valueSpecMapping;
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
     * Delete item with given type and also connected object
     * 
     */
    public static class DeleteItem {
        String itemType;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }
    }

    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

    WhenConditionConfig when;
    
    List<DeleteItem> delete;

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

    public List<DeleteItem> getDelete() {
        return delete;
    }

    public void setDelete(List<DeleteItem> deleteItemList) {
        this.delete = deleteItemList;
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
