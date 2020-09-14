
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
    typeRuleSetMap: TypeRuleSetMap;
    rules: ApViewSettingRuleMap;
}

interface TypeRuleSetMap {
    [id: number]: number;
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
