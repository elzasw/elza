
export interface PartOrder {
    code: string;
}

export interface ItemType {
    code: string;
    position?: number;
    width: number;
    partType?: string;
    geoSearchItemType?: string;
}

export interface ApViewSettings {
    /** Rule set of each scope: an entity is shown with the settings of the rule set of its scope. */
    scopeRuleSetMap: ScopeRuleSetMap;
    rules: ApViewSettingRuleMap;
}

interface ScopeRuleSetMap {
    [scopeId: number]: number;
}

interface ApViewSettingRuleMap {
    [id: number]: ApViewSettingRule;
}

export interface ApViewSettingRule {
    ruleSetId: number;
    code: string;
    partsOrder: PartOrder[];
    itemTypes: ItemType[];
}
