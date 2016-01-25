/**
 * Utility pro pořádání.
 */
import {indexById} from 'stores/app/utils.jsx'

/**
 * Vytvoření virtuálního kořenového uzlu pro kořenový uzel FA.
 * @param {Object} fa fa
 * @param {Object} rootNode kořenový node fa
 * @return {Object} virtuální kořenový uzel pro kořenový uzel FA
 */
export function createFaRoot(fa, rootNode) {
    return {id: 'ROOT_' + rootNode.id, name: fa.name, root: true};
}

/**
 * Zjištění, že id je id virtuálního kořenového uzlu pro kořenový uzel FA.
 * @param {Integer} nodeId node id
 * @return {Boolean} true, pokud se jedná o id virtuálního kořenového uzlu pro kořenový uzel FA
 */
export function isFaRootId(nodeId) {
    var isRoot = false;
    if (typeof nodeId == 'string') {    // pravděpodobně root
        if (nodeId.substring(0, 5) == 'ROOT_') {
            isRoot = true;
        }
    }

    return isRoot;
}

/**
 * Načtení nadřazeného uzlu k předanému.
 * @param node {Object} uzel, pro který chceme vrátit nadřazený
 * @param faTreeNodes {Array} seznam načtených uzlů pro data stromu
 * @return {Object} parent nebo null, pokud je předaný uzel kořenový
 */
export function getParentNode(node, faTreeNodes) {
    var index = indexById(faTreeNodes, node.id);
    while (--index >= 0) {
        if (faTreeNodes[index].depth < node.depth) {
            return faTreeNodes[index];
        }
    }
    return null;
}