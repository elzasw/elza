
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

function findByNodeKeyInGlobalState(globalState, versionId, nodeKey) {
    var fundIndex = indexById(globalState.arrRegion.funds, versionId, "versionId");
    if (fundIndex != null) {
        var nodes = globalState.arrRegion.funds[fundIndex].nodes.nodes;
        for (var a=0; a<nodes.length; a++) {
            if (nodes[a].nodeKey == nodeKey) {
                return {fundIndex: fundIndex, node: nodes[a], nodeIndex: a};
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

