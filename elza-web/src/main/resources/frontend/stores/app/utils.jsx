
function findByNodeKeyInNodes(nodesState, versionId, nodeKey) {
    var nodes = nodesState.nodes;
    for (var a=0; a<nodes.length; a++) {
        if (nodes[a].nodeKey == nodeKey) {
            return {node: nodes[a], nodeIndex: a};
        }
    }
    return null;
}
exports.findByNodeKeyInNodes = findByNodeKeyInNodes

function getNodeKeyType(nodeKey) {
    const i = nodeKey.indexOf('|')
    return i === -1 ? nodeKey : nodeKey.substring(0, i)
}
exports.getNodeKeyType = getNodeKeyType

function findByNodeKeyInGlobalState(globalState, versionId, nodeKey) {
    var fundIndex = indexById(globalState.arrRegion.funds, versionId, "versionId");
    if (fundIndex != null) {
        const fund = globalState.arrRegion.funds[fundIndex]
        var nodes = fund.nodes.nodes
        for (var a=0; a<nodes.length; a++) {
            if (nodes[a].nodeKey == nodeKey) {
                return {fundIndex: fundIndex, fund: fund, node: nodes[a], nodeIndex: a};
            }
        }
    }
    return null;
}
exports.findByNodeKeyInGlobalState = findByNodeKeyInGlobalState

function getMapFromList(list, attrName = 'id') {
    var map = {}
    list.forEach(x => {
        map[x[attrName]] = x
    })
    return map
}
exports.getMapFromList = getMapFromList

function getSetFromIdsList(list) {
    var map = {}
    list && list.forEach(x => {
        map[x] = true
    })
    return map
}
exports.getSetFromIdsList = getSetFromIdsList

function indexById(arr, id, attrName = 'id') {
    if (arr == null) {
        return null;
    }

    for (var a = 0; a < arr.length; a++) {
        if (arr[a][attrName] == id) {
            return a;
        }
    }
    return null;
}
exports.indexById = indexById

function objectById(arr, id, attrName = 'id') {
    if (arr == null) {
        return null;
    }

    for (var a = 0; a < arr.length; a++) {
        if (arr[a][attrName] == id) {
            return arr[a];
        }
    }
    return null;
}
exports.objectById = objectById

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
exports.selectedAfterClose = selectedAfterClose

