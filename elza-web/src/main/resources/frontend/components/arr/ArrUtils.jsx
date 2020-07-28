/**
 * Utility pro pořádání.
 */
import {getSetFromIdsList, indexById} from 'stores/app/utils.jsx'
import React from 'react';
import {dateTimeToString, dateToString} from 'components/Utils.jsx'
import {i18n} from 'components/shared';

/**
 * Načtení stromového uspořádání - kořenové jsou item group a pod nimi item types. Do stromu se vkládají jen položky, které ještě nejsou použivté v descItemGroups.
 * Metoda slouží pro vytvoření dat pro přidání do formuláře pořádání (JP a Output).
 * @param descItemGroups již použité položky (pod skupinami)
 * @param infoTypesMapInput infor types
 * @param refTypesMapInput ref types
 * @param infoGroups info group
 * @param strictMode použít striktní mód a nezařazovat nemožné item type
 * @return {Array} strom
 */
export function getDescItemsAddTree(descItemGroups, infoTypesMapInput, refTypesMapInput, infoGroups, strictMode = false) {
    // Pro přidání chceme jen ty, které zatím ještě nemáme
    let infoTypesMap = {...infoTypesMapInput};

    descItemGroups.forEach(group => {
        group.descItemTypes.forEach(descItemType => {
            delete infoTypesMap[descItemType.id];
        });
    });

    // Sestavení seznamu včetně skupin
    let descItemTypes = [];
    infoGroups.forEach(infoGroup => {
        const itemTypes = [];
        infoGroup.types.forEach(infoType => {
            const itemType = infoTypesMap[infoType.id];
            if (itemType) {    // ještě ji na formuláři nemáme
                // v nestriktním modu přidáváme všechny jinak jen možné
                if (!strictMode || itemType.type !== 'IMPOSSIBLE') {
                    itemTypes.push({
                        // nový item type na základě původního z refTables
                        ...refTypesMapInput[infoType.id],
                        // obohacení o aktualizované stavy ze serveru
                        ...itemType
                    });
                }
            }
        });

        if (itemTypes.length > 0) { // nějaké položky máme, přidáme skupinu i s položkami
            descItemTypes.push({
                groupItem: true,
                id: infoGroup.code,
                name: infoGroup.code === "DEFAULT" ? i18n("subNodeForm.descItemGroup.default") : infoGroup.name,
                children: itemTypes
            });
        }
    });

    return descItemTypes;
}

export function getFundFromFundAndVersion(fund, version) {
    var fundVersionClosed = version.lockDate != null;
    var fund = Object.assign({}, fund, {versionId: version.id, lockDate: version.lockDate, activeVersion: version, closed: fundVersionClosed});
    return fund;
}

export function getSpecsIds(refType, selectionType, selectedIds) {
    let specIds = [];
    if (refType.useSpecification) {
        if (selectionType === 'selected') {
            specIds = selectedIds;
        } else {
            let set = getSetFromIdsList(selectedIds);
            refType.descItemSpecs.forEach(i => {
                if ((!set[i.id] && selectionType === 'unselected') || (!set[i.id] && selectionType === 'unselected')) {
                    specIds.push(i.id);
                }
            });
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
    const version = typeof fund.versionId !== 'undefined' ? fund.versionId : fund.fundVersionId;
    return {id: 'ROOT_' + version, name: fund.name, root: true};
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
    return createReferenceMarkFromArray(node.referenceMark, elProps)
}

/**
 * Vytvoření referenčního označení.
 *
 * @param referenceMark {Array} reference mark array
 * @param elProps {Object} další properties pro přidání do renderovaných elementů
 */
export function createReferenceMarkFromArray(referenceMark, elProps) {
    var levels = [];

    if (referenceMark) {
        referenceMark.forEach((i, index) => {
            if (index % 2 == 0) {
                if (i < 1000) {
                    var cls = "level";
                    if (i > 999) {
                        cls = "level small";
                    }
                    levels.push(<span {...elProps} key={'level' + index} className={cls}>{i}</span>)
                } else {
                    var iStr = i + "";
                    levels.push(<span {...elProps} key={'level' + index} title={i} className="level">_{iStr.substr(-2)}</span>)
                }
            } else {
                if (index + 1 < referenceMark.length) {
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
    if (node && node.referenceMark) {
        return createReferenceMarkStringFromArray(node.referenceMark);
    } else {
        return 0;
    }
}

/**
 * Vytvoření referenčního označení - textově.
 *
 * @param referenceMark {Array} pole reference mark
 */
export function createReferenceMarkStringFromArray(referenceMark) {
    return referenceMark.join(" ");
}

/**
 * Vytvoření názvu požadavku na digitalizaci pro zorbazení uživateli.
 * @param digitizationRequest objekt požadavku
 * @param userDetail detail přihlášeného uživatele
 */
export function createDigitizationName(digitizationRequest, userDetail) {
    // Uživatelské jméno chceme pouze pokud je definované nebo je jiné než přihlášený uživatel
    const usernameTmp = digitizationRequest.username ? digitizationRequest.username : "System";
    const username = userDetail ? (usernameTmp !== userDetail.username ? usernameTmp : null) : usernameTmp;
    const usernameStr = username ? "[" + username + "] " : "";
    let text = usernameStr + dateTimeToString(new Date(digitizationRequest.create));
    if (digitizationRequest.nodesCount != null) {
        text += " (" + digitizationRequest.nodesCount + ")";
    }
    return text;
}

/**
 * Vytvoření názvu požadavku na link/unlink dao.
 *
 * @param daoLinkRequest objekt požadavku
 * @param userDetail detail přihlášeného uživatele
 */
export function createDaoLinkName(daoLinkRequest, userDetail) {
    let text = "";
    text += i18n('arr.request.title.type.DAO_LINK.' + daoLinkRequest.type);
    text += " " + daoLinkRequest.didCode;
    return text;
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

export function getNodeIcon(colorCoded, iconCode) {
    let iconStyle = {};
    let backgroundColor, color;

    if (colorCoded){
        if (COLOR_MAP[iconCode]){
            backgroundColor = COLOR_MAP[iconCode].background;
            color = COLOR_MAP[iconCode].color;
        } else {
            backgroundColor = COLOR_MAP["default"].background;
            color = COLOR_MAP["default"].color;
        }

        iconStyle = {
            backgroundColor:backgroundColor,
            color:color
        };
    }

    let icon = getGlyph(iconCode);

    if (ICON_REMAP[iconCode] && colorCoded){
        icon = ICON_REMAP[iconCode];
    }

    return ({
        glyph: icon,
        style: iconStyle,
        fill: iconStyle.backgroundColor,
        stroke: "none"
    });
}

export const ICON_REMAP = {
    "fa-folder-o":"folder",
    "ez-serie":"serie",
    "fa-sitemap":"sitemap",
    "fa-file-text-o":"fileText",
    "ez-item-part-o":"fileTextPart",
    "fa-exclamation-triangle":"triangleExclamation"
};

const COLOR_MAP = {
    "fa-database":{background:"#fff",color:"#000"},
    "fa-folder-o":{background:"#ffcc00",color:"#fff"},
    "ez-serie":{background:"#6696dd", color:"#fff"},
    "fa-sitemap":{background:"#4444cc", color:"#fff"},
    "fa-file-text-o":{background:"#ff972c", color:"#fff"},
    "ez-item-part-o":{background:"#cc3820", color: "#fff"},
    "default":{background:"#333", color: "#fff"}
}

export const DIGITIZATION = "DIGITIZATION";
export const DAO = "DAO";
export const DAO_LINK = "DAO_LINK";
export function getRequestType(digReq) {
    switch (digReq["@class"]) {
        case ".ArrDigitizationRequestVO":
            return DIGITIZATION;
        case ".ArrDaoLinkRequestVO":
            return DAO_LINK;
        case ".ArrDaoRequestVO":
            return DAO;
    }
    return null;
}

export function hasDescItemTypeValue(dataType) {
    switch (dataType.code) {
        case 'TEXT':
        case 'STRING':
        case 'INT':
        case 'DATE':
        case 'COORDINATES':
        case 'DECIMAL':
        case 'PARTY_REF':
        case 'RECORD_REF':
        case 'URI_REF':
        case 'STRUCTURED':
        case 'JSON_TABLE':
        case 'FORMATTED_TEXT':
        case 'UNITDATE':
        case 'UNITID':
            return true;
        case 'ENUM':
            return false;
        default:
            console.error("Unsupported data type", dataType);
            return false;
    }
}
