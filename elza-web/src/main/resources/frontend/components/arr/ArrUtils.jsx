/**
 * Utility pro pořádání.
 */
import {indexById} from 'stores/app/utils.jsx'
import React from 'react';
import ReactDOM from 'react-dom';
import {getSetFromIdsList} from 'stores/app/utils.jsx'

export function getFundFromFundAndVersion(fund, version) {
    var fundVersionClosed = version.lockDate != null;
    var fund = Object.assign({}, fund, {versionId: version.id, lockDate: version.lockDate, activeVersion: version, closed: fundVersionClosed});
    return fund;
}

export function getSpecsIds(refType, selectionType, selectedIds) {
    var specIds = []
    if (refType.useSpecification) {
        if (selectionType === 'selected') {
            specIds = selectedIds
        } else {
            var set = getSetFromIdsList(selectedIds)
            refType.descItemSpecs.forEach(i => {
                if (!set[i.id]) {
                    specIds.push(i.id)
                }
            })
        }
    }
    return specIds
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

export function getNodeFirstChild(nodes, nodeId) {
    var result = null;

    var index = indexById(nodes, nodeId);
    if (index != null) {
        var depth = nodes[index].depth;
        index++;
        if (index < nodes.length && nodes[index].depth == depth + 1) {
            return nodes[index]
        }
    }

    return null;
}

export function getNodePrevSibling(nodes, nodeId) {
    var index = indexById(nodes, nodeId);
    if (index === null) {
        return null;
    }
    var node = nodes[index];
    index--;
    while (index >= 0) {
        if (nodes[index].depth === node.depth) {
            break;
        }
        index--;
    }

    if (index >= 0) {
        return nodes[index]
    } else {
        return null
    }
}

export function getNodeNextSibling(nodes, nodeId) {
    var index = indexById(nodes, nodeId);
    if (index === null) {
        return null;
    }
    var node = nodes[index];
    index++;
    while (index < nodes.length) {
        if (nodes[index].depth === node.depth) {
            break;
        }
        index++;
    }

    if (index < nodes.length) {
        return nodes[index]
    } else {
        return null
    }
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
 * Uložení nastavení.
 *
 * @param items seznam itemů
 * @param id    id konkrétního nastavení
 * @param newItem  položka, kterou ukládám
 * @returns {Array} seznam itemů
 */
export function setSettings(items, id, newItem) {
    if (items == null) {
        items = [];
        items.push(newItem);
        return items;
    }

    if (id == null) {
        items.push(newItem);
        return items;
    }

    for (let i = 0; i < items.length; i++) {
        let item = items[i];
        if (item.id == id) {
            items[i] = newItem;
            break;
        }
    }

    return items;
}

/**
 * Vrací konkrétní položku nastavení.
 *
 * @param items      seznam itemů
 * @param type       typ nastavení
 * @param entityType typ entity
 * @param entityId   identifikátor entity
 * @returns {Object} objekt itemu
 */
export function getOneSettings(items, type = null, entityType = null, entityId = null) {

    if (items != null) {
        for (let i = 0; i < items.length; i++) {
            let item = items[i];
            if ((type == null || type == item.settingsType)
                && (entityType == null || entityType == item.entityType)
                && (entityId == null || entityId == item.entityId)
            ) {
                return item;
            }
        }
    }

    return {entityType: entityType, entityId: entityId, settingsType: type, value: null}
}

/**
 * Získání uživatelského nastavení.
 *
 * @param items      seznam itemů
 * @param type       typ nastavení
 * @param entityType typ entity
 * @param entityId   identifikátor entity
 * @returns {Array} seznam itemů
 */
export function getSettings(items, type = null, entityType = null, entityId = null) {
    const result = [];

    if (items == null) {
        return result;
    }

    items.map((item) => {
        if ((type == null || type == item.settingsType)
            && (entityType == null || entityType == item.entityType)
            && (entityId == null || entityId == item.entityId)
        ) {
            result.push(item);
        }
    });

    return result;
}

/**
 * Vytvoření virtuálního kořenového uzlu pro kořenový uzel AS.
 * @param {Object} fund fund
 * @return {Object} virtuální kořenový uzel pro kořenový uzel AS
 */
export function createFundRoot(fund) {
    return {id: 'ROOT_' + fund.versionId, name: fund.name, root: true};
}

/**
 * Zjištění, že id je id virtuálního kořenového uzlu pro kořenový uzel AS.
 * @param {Integer} nodeId node id
 * @return {Boolean} true, pokud se jedná o id virtuálního kořenového uzlu pro kořenový uzel AS
 */
export function isFundRootId(nodeId) {
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
 * @param fundTreeNodes {Array} seznam načtených uzlů pro data stromu
 * @return {Object} parent nebo null, pokud je předaný uzel kořenový
 */
export function getParentNode(node, fundTreeNodes) {
    var index = indexById(fundTreeNodes, node.id);
    while (--index >= 0) {
        if (fundTreeNodes[index].depth < node.depth) {
            return fundTreeNodes[index];
        }
    }
    return null;
}

/**
 * Vytvoření referenčního označení.
 *
 * @param node {Object} jednotka popisu
 * @param elProps {Object} další properties pro přidání do renderovaných elementů
 */
export function createReferenceMark(node, elProps) {
    var levels = [];

    if (node.referenceMark) {
        node.referenceMark.forEach((i, index) => {
            if (index % 2 == 0) {
                if (i < 1000) {
                    var cls = "level";
                    if (i > 99) {
                        cls = "level small";
                    }
                    levels.push(<span {...elProps} key={'level' + index} className={cls}>{i}</span>)
                } else {
                    var iStr = i + "";
                    levels.push(<span {...elProps} key={'level' + index} title={i} className="level small">.{iStr.substr(-3)}</span>)
                }
            } else {
                if (index + 1 < node.referenceMark.length) {
                    levels.push(<span {...elProps} key={'sep' + index} className="separator">{i}</span>)
                }
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
    return node && node.referenceMark && node.referenceMark.join(" ");
}

/**
 * Pokud je ikona null, je použita výchozí.
 *
 * @param type ikona zobrazení
 */
export function getGlyph(type) {
    if (type == null) {
        return "fa-exclamation-triangle"
    } else {
        return type;
    }
}

export function hasDescItemTypeValue(dataType) {
    switch (dataType.code) {
        case 'TEXT':
        case 'STRING':
        case 'INT':
        case 'COORDINATES':
        case 'DECIMAL':
        case 'PARTY_REF':
        case 'RECORD_REF':
        case 'PACKET_REF':
        case 'JSON_TABLE':
        case 'FORMATTED_TEXT':
        case 'UNITDATE':
        case 'UNITID':
            return true
        case 'ENUM':
            return false
        default:
            console.error("Unsupported data type", dataType);
            return false;
    }
}