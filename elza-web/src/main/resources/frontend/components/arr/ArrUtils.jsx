/**
 * Utility pro pořádání.
 */

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


