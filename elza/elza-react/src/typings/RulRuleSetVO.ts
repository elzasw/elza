export enum RuleType {
    ENTITY = 'ENTITY',
    ARRANGEMENT = 'ARRANGEMENT',
}

export interface GridView {
    /**
     * Kód atributu.
     */
    // code: string;

    /**
     * id atributu.
     */
    id: number;

    /**
     * Zobrazit ve výchozím zobrazení?
     */
    showDefault: boolean;

    /**
     * Výchozí šířka.
     */
    width: number;
}

export interface RulRuleSetVO {
    id: number;

    code: string;

    name: string;

    ruleType: RuleType;

    /** Kódy atributů pro zobrazení v gridu hromadných úprav */
    gridViews: GridView[];
}
