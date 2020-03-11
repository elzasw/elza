function findByRoutingKeyInNodes(nodesState, versionId, routingKey) {
    const nodes = nodesState.nodes;
    for (let a = 0; a < nodes.length; a++) {
        // TODO May need revision for type comparison
        // FIXME
        if (nodes[a].routingKey == routingKey)
            return { node: nodes[a], nodeIndex: a };
    }
    return null;
}

exports.findByRoutingKeyInNodes = findByRoutingKeyInNodes;

function getRoutingKeyType(routingKey) {
    const i = routingKey.indexOf('|');
    return i === -1 ? routingKey : routingKey.substring(0, i);
}

exports.getRoutingKeyType = getRoutingKeyType;

function findByRoutingKeyInGlobalState(globalState, versionId, routingKey) {
    const fundIndex = indexById(globalState.arrRegion.funds, versionId, 'versionId');
    if (fundIndex != null) {
        const fund = globalState.arrRegion.funds[fundIndex];
        const nodes = fund.nodes.nodes;
        for (let a = 0; a < nodes.length; a++) {
            // TODO May need revision for type comparison
            // FIXME
            if (nodes[a].routingKey == routingKey)
                return { fundIndex: fundIndex, fund: fund, node: nodes[a], nodeIndex: a };
        }
    }
    return null;
}

exports.findByRoutingKeyInGlobalState = findByRoutingKeyInGlobalState;

function getMapFromList(list, attrName = 'id') {
    let map = {};
    list.forEach(x => {
        map[x[attrName]] = x;
    });
    return map;
}

exports.getMapFromList = getMapFromList;

function getSetFromIdsList(list) {
    let map = {};
    list && list.forEach(x => {
        map[x] = true;
    });
    return map;
}

exports.getSetFromIdsList = getSetFromIdsList;

function getIdsList(objectList, attrName = 'id') {
    return objectList.map(obj => obj[attrName]);
}

exports.getIdsList = getIdsList;

function indexById(arr, id, attrName = 'id') {
    if (arr == null)
        return null;

    for (let a = 0; a < arr.length; a++) {
        const uuid = arr[a][attrName];
        if (uuid === id || (!uuid && !id))
            return a;
    }
    return null;
}

exports.indexById = indexById;

function objectById(arr, id, attrName = 'id') {
    if (arr == null)
        return null;

    for (let a = 0; a < arr.length; a++) {
        const uuid = arr[a][attrName];
        if (uuid === id || (!uuid && !id))
            return arr[a];
    }
    return null;
}

exports.objectById = objectById;

function selectedAfterClose(arr, index) {
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

exports.selectedAfterClose = selectedAfterClose;

