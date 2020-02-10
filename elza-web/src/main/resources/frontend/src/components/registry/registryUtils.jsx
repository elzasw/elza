/**
 * Dohledání node podle id.
 * @param node node, pro který se id testuje včetně jeho potomků
 * @param id id
 * @return {*} node nebo null
 */
function _getNodeById (node, id) {
    if (node.id === id) {
        return node;
    }

    if (node.children) {
        for (let a=0; a<node.children.length; a++) {
            const n = _getNodeById(node.children[a], id);
            if (n) {
                return n;
            }
        }
    }

    return null;
}

/**
 * Dohledání položky ve stromu dle předaného id, jedná se o strom rejstříků.
 * @param id id nebo null
 * @param items stromová reprezentace položek - pole kořenových
 * @return položka nebo null
 */
export function getTreeItemById(id, items) {
    if (typeof id === "undefined" || id === "" || id === null) {
        return null;
    }

    for (let a=0; a<items.length; a++) {
        const n = _getNodeById(items[a], id);
        if (n) {
            return n;
        }
    }

    return null;
}
