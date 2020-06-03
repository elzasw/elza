export function findByRoutingKeyInNodes(nodesState, versionId, routingKey) {
    const nodes = nodesState.nodes;
    for (let a = 0; a < nodes.length; a++) {
        // TODO May need revision for type comparison
        // FIXME
        if (nodes[a].routingKey == routingKey) return {node: nodes[a], nodeIndex: a};
    }
    return null;
}

export function getRoutingKeyType(routingKey) {
    const i = routingKey.indexOf('|');
    return i === -1 ? routingKey : routingKey.substring(0, i);
}

export function findByRoutingKeyInGlobalState(globalState, versionId, routingKey) {
    const fundIndex = indexById(globalState.arrRegion.funds, versionId, 'versionId');
    if (fundIndex != null) {
        const fund = globalState.arrRegion.funds[fundIndex];
        const nodes = fund.nodes.nodes;
        for (let a = 0; a < nodes.length; a++) {
            // TODO May need revision for type comparison
            // FIXME
            if (nodes[a].routingKey == routingKey)
                return {fundIndex: fundIndex, fund: fund, node: nodes[a], nodeIndex: a};
        }
    }
    return null;
}

export function getMapFromList(list, attrName = 'id') {
    let map = {};
    list.forEach(x => {
        map[x[attrName]] = x;
    });
    return map;
}

export function getSetFromIdsList(list) {
    let map = {};
    list &&
        list.forEach(x => {
            map[x] = true;
        });
    return map;
}

export function getIdsList(objectList, attrName = 'id') {
    return objectList.map(obj => obj[attrName]);
}

export function indexById(arr, id, attrName = 'id') {
    if (arr == null) return null;

    for (let a = 0; a < arr.length; a++) {
        const uuid = arr[a][attrName];
        if (uuid === id || (!uuid && !id)) return a;
    }
    return null;
}

export function objectById(arr, id, attrName = 'id') {
    if (arr == null) return null;

    for (let a = 0; a < arr.length; a++) {
        const uuid = arr[a][attrName];
        if (uuid === id || (!uuid && !id)) return arr[a];
    }
    return null;
}

export function selectedAfterClose(arr, index) {
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

export function flatRecursiveMap(map, prop = 'children') {
    let result = {};

    const keys = Object.keys(map);
    for (let i = 0; i < keys.length; i++) {
        const key = keys[i];
        const value = map[key];
        result[key] = value;
        if (value[prop]) {
            result = {
                ...result,
                ...flatRecursiveMap(getMapFromList(value[prop]))
            }
        }
    }

    return result;
}
