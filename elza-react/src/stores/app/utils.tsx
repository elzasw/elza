import { AppState, NodesState } from "typings/store";

export function findByRoutingKeyInNodes(nodesState?: NodesState, _versionId?: string, routingKey?: string) {
    if(nodesState?.nodes){
        const nodes = nodesState.nodes;
        for (let a = 0; a < nodes.length; a++) {
            if (nodes[a].routingKey === routingKey) 
            return {
                node: nodes[a], 
                nodeIndex: a
            };
        }
    }
    return null;
}

export function getRoutingKeyType(routingKey: string) {
    const delimiterIndex = routingKey.indexOf('|');
    return delimiterIndex === -1 ? routingKey : routingKey.substring(0, delimiterIndex);
}

export function findByRoutingKeyInGlobalState(globalState: AppState, versionId: number, routingKey: string) {
    const fundIndex = indexByProperty(globalState.arrRegion.funds, versionId, 'versionId');
    if (fundIndex != null) {
        const fund = globalState.arrRegion.funds[fundIndex];
        const nodes = fund.nodes.nodes;
        for (let a = 0; a < nodes.length; a++) {
            if (nodes[a].routingKey === routingKey)
                return {
                    fundIndex: fundIndex, 
                    fund: fund, 
                    node: nodes[a], 
                    nodeIndex: a
                };
        }
    }
    return null;
}


export function getMapFromList<T extends {id?: string | number}, P extends keyof T>(list: T[]) : Record<string | number, T>
export function getMapFromList<T, P extends keyof T>(list: T[], propertyName:P) : Record<string | number, T>
export function getMapFromList<T>(list: T[], propertyName: string = "id") : Record<string | number, T> {
    let map: Record<string | number, T> = {};
    list && 
        list.forEach(listItem => {
            const propertyValue = listItem[propertyName];
            if(typeof propertyValue === "string" || typeof propertyValue === "number"){
                map[propertyValue] = listItem;
            } else {
                throw new Error(`'getMapFromList' error. Value of '${propertyName}' is not of type 'string' or 'number'`)
            }
        });
    return map;
}

export function getSetFromIdsList(list:Array<string | number> = []) : Record<string | number, boolean> {
    const map:Record<string | number, boolean> = {};
    list.forEach((x) => { map[x] = true; });
    return map;
}

export function getIdsList<T extends {id: string | number}>(objectList:T[], attrName: keyof T = 'id') {
    return objectList.map(obj => obj[attrName]);
}

export function indexByProperty<T, P extends keyof T>(array: T[] | null | undefined = [], value: T[P] | null | undefined, propertyName: P) : number | null {
    if ( isNullOrUndefined(array) ) return null;
    
    const index = array.findIndex((item) => item[propertyName] !== undefined ? item[propertyName] === value : false );

    return index !== - 1 ? index : null;
}

export function objectByProperty<T, P extends keyof T>(array: T[] | null | undefined = [], value: T[P], propertyName: P) : T | null {
    if ( isNullOrUndefined(array) ) return null;

    const object = array.find((item) => item[propertyName] ? item[propertyName] === value : false );
    return object || null;
}

/*
 * @Obsolete - nahradit funkcí indexByProperty
 * Otypovano tak, aby odpovidalo soucasne funkcionalite
 */
export function indexById<T extends {id?: string | number}>(arr: T[] | null | undefined, id: string | number | null | undefined) : number | null
export function indexById<T, P extends keyof T>(arr: T[] | null | undefined, id: T[P] | undefined, propertyName: P) : number | null
export function indexById(arr: any[] | null | undefined, id?: any, propertyName: any = "id") : number | null {
    return indexByProperty(arr, id, propertyName);
}

/*
 * @Obsolete - nahradit funkcí objectByProperty
 * Otypovano tak, aby odpovidalo soucasne funkcionalite
 */
export function objectById<T extends {id?: string | number}>(arr: T[] | null | undefined, id: string | number | null | undefined) : T | null
export function objectById<T, P extends keyof T>(arr: T[] | null | undefined, id: T[P] | undefined, propertyName: P) : T | null
export function objectById(arr: any[] | null | undefined, id: any, propertyName: any = "id") : any | null {
    return objectByProperty(arr, id, propertyName) || null;
}

export function selectedAfterClose<T>(arr: T[], index: number) {
    if (index >= arr.length - 1) {
        if (index - 1 >= 0) {
            return index - 1;
        } else {
            return null;
        }
    } else {
        return index;
    }
}

// export function flatRecursiveMap(map, prop = 'children') {
export function flatRecursiveMap<T extends { children?: T[] }, P extends keyof T>(map: Record<string | number, T>) : Record<string | number, T>
export function flatRecursiveMap<T, P extends keyof T>(map: Record<string | number, T>, prop:P) : Record<string | number, T>
export function flatRecursiveMap(map: Record<string | number, any>, prop: string = "children") {
    let result = {};

    Object.keys(map).forEach((key)=>{
        const value = map[key];
        result[key] = value;

        const children = value[prop];
        if (children) {
            const childMap = getMapFromList<any, any>(value[prop]);
            result = {
                ...result,
                ...flatRecursiveMap(childMap, prop),
            }
        }
    })

    return result;
}

function isNullOrUndefined<T>(value: T | null | undefined): value is null | undefined {
    return value === null || value === undefined;
}
