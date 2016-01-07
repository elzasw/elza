
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
    var faIndex = indexById(globalState.arrRegion.fas, versionId, "versionId");
    if (faIndex != null) {
        var nodes = globalState.arrRegion.fas[faIndex].nodes.nodes;
        for (var a=0; a<nodes.length; a++) {
            if (nodes[a].nodeKey == nodeKey) {
                return {faIndex: faIndex, node: nodes[a], nodeIndex: a};
            }
        }
    }
    return null;
}
exports.findByNodeKeyInGlobalState = findByNodeKeyInGlobalState

function indexById(arr, id, attrName = null) {
    if (arr == null) {
        return null;
    }

    if (attrName !== null) {
        for (var a=0; a<arr.length; a++) {
            if (arr[a][attrName] == id) {
                return a;
            }
        }
    } else {
        for (var a=0; a<arr.length; a++) {
            if (arr[a].id == id) {
                return a;
            }
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
