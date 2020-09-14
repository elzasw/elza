package cz.tacr.elza.controller.vo.ap;

import cz.tacr.elza.packageimport.xml.SettingItemTypes;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;

import java.util.List;
import java.util.Map;

public class ApViewSettings {

    private Map<Integer, ApViewSettingsRule> rules;
    private Map<Integer, Integer> typeRuleSetMap;

    public Map<Integer, ApViewSettingsRule> getRules() {
        return rules;
    }

    public void setRules(final Map<Integer, ApViewSettingsRule> rules) {
        this.rules = rules;
    }

    public Map<Integer, Integer> getTypeRuleSetMap() {
        return typeRuleSetMap;
    }

    public void setTypeRuleSetMap(final Map<Integer, Integer> typeRuleSetMap) {
        this.typeRuleSetMap = typeRuleSetMap;
    }

    public static class ApViewSettingsRule {
        private Integer ruleSetId;
        private String code;
        private List<SettingPartsOrder.Part> partsOrder;
        private List<SettingItemTypes.ItemType> itemTypes;

        public Integer getRuleSetId() {
            return ruleSetId;
        }

        public void setRuleSetId(final Integer ruleSetId) {
            this.ruleSetId = ruleSetId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public List<SettingPartsOrder.Part> getPartsOrder() {
            return partsOrder;
        }

        public void setPartsOrder(final List<SettingPartsOrder.Part> partsOrder) {
            this.partsOrder = partsOrder;
        }

        public List<SettingItemTypes.ItemType> getItemTypes() {
            return itemTypes;
        }

        public void setItemTypes(final List<SettingItemTypes.ItemType> itemTypes) {
            this.itemTypes = itemTypes;
        }

    }
}
