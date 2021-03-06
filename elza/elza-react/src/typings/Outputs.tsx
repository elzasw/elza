export interface ArrOutputVO {
    id: number;
    internalCode: string;
    name: string;
    state: string;
    error: any;
    nodes: TreeNodeVO[];
    outputTypeId: number;
    templateIds: number[];
    outputResultIds: number[];
    generatedDate: any;
    version: number;
    outputSettings: any;
    createDate: string;
    deleteDate: any;
    scopes: ApScopeVO[];
    anonymizedAp: any;
}
export interface BaseCodeVO {
    /**
     * identifikátor
     */
    id: number;

    /**
     * kód
     */
    code: string;

    /**
     * název
     */
    name: string;
}

export interface ApScopeVO extends BaseCodeVO {
    language: string;
}

export interface TreeNodeVO {
    id: number;
    depth: number;
    name: string;
    icon: string;
    hasChildren: boolean;
    referenceMark: string[];
    version: number;
    arrPerm: boolean;
}

export enum OutputState {
    /**
     * Rozpracovaný
     */
    OPEN = 'OPEN',
    /**
     * Běží hromadná akce
     */
    COMPUTING = 'COMPUTING',
    /**
     * Generování
     */
    GENERATING = 'GENERATING',
    /**
     * Vygenerovaný
     */
    FINISHED = 'FINISHED',
    /**
     * Vygenerovaný neaktuální
     */
    OUTDATED = 'OUTDATED',
}
