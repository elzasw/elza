/**
 * Utility pro pořádání.
 */
import {indexById} from 'stores/app/utils.jsx'
import React from 'react';
import ReactDOM from 'react-dom';

export function getFaFromFaAndVersion(fa, version) {
    var fa = Object.assign({}, fa, {faId: fa.id, versionId: version.id, id: version.id, activeVersion: version});
    return fa;
}

export function getNodeParent(nodes, nodeId) {
    var result = null;

    var index = indexById(nodes, nodeId);
    if (index != null) {
        var depth = nodes[index].depth;
        index--;
        while (index >= 0) {
            var n = nodes[index];
            if (n.depth < depth) {
                result = n;
                break;
            }
            index--;
        }
    }

    return result;
}

export function getNodeParents(nodes, nodeId) {
    var result = [];

    var index = indexById(nodes, nodeId);
    if (index != null) {
        var depth = nodes[index].depth;
        index--;
        while (index >= 0) {
            var n = nodes[index];
            if (n.depth < depth) {
                result.push(n);
                depth = n.depth;
            }
            index--;
        }
    }

    return result;
}

/**
 * Vytvoření virtuálního kořenového uzlu pro kořenový uzel FA.
 * @param {Object} fa fa
 * @return {Object} virtuální kořenový uzel pro kořenový uzel FA
 */
export function createFaRoot(fa) {
    return {id: 'ROOT_' + fa.versionId, name: fa.name, root: true};
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

/**
 * Vytvoření referenčního označení.
 *
 * @param node {Object} jednotka popisu
 */
export function createReferenceMark(node) {
    var levels = [];
    if (node.referenceMark) {
        node.referenceMark.forEach((i, index) => {
            if (i < 1000) {
                var cls = "level";
                if (i > 99) {
                    cls = "level small";
                }
                levels.push(<span key={'level' + index} className={cls}>{i}</span>)
            } else {
                levels.push(<span key={'level' + index} className="level">.{i % 1000}</span>)
            }
            if (index + 1 < node.referenceMark.length) {
                levels.push(<span key={'sep' + index} className="separator"></span>)
            }
        });
    }
    return levels;
}

/**
 * Vytvoření referenčního označení - textově.
 *
 * @param node {Object} jednotka popisu
 */
export function createReferenceMarkString(node) {
    return node && node.referenceMark && node.referenceMark.join(" | ");
}

/**
 * Nastavuje typ ikony podle kódu zobrazení.
 *
 * @param type kódu zobrazení
 */
export function getGlyph(type) {
    // TODO slapa: dopsat typy podle serveru

    switch (type) {
        default:
            return "fa-exclamation-triangle"
    }
}