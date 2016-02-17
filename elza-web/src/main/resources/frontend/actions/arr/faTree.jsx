/**
 * Akce pro strom AP.
 * Vysvětlení pojmů:
 * uzel - JP
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {faSelectSubNode} from './nodes'

// jen vyber polozky, vyuzite jen v presunech JP
export function faTreeSelectNode(area, nodeId, ctrl, shift, newFilterCurrentIndex = null) {
    return {
        type: types.FA_FA_TREE_SELECT_NODE,
        area,
        nodeId,
        ctrl,
        shift,
        newFilterCurrentIndex
    }
}

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 * @param {boolean} pokud je true, je pod rozbalovaný uzel přidán nový uzel s informací, že se načítají data
 */
export function _faTreeNodeExpand(area, node, addWaitingNode=false) {
    return {
        type: types.FA_FA_TREE_EXPAND_NODE,
        area,
        node,
        addWaitingNode,
    }
}

/**
 * Zabalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeCollapse(area, node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE,
        area,
        node
    }
}

/**
 * Hledání ve stromu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AP
 * @param {string} filterText hledaný text
 */
export function faTreeFulltextChange(area, versionId, filterText) {
    return {
        type: types.FA_FA_TREE_FULLTEXT_CHANGE,
        area,
        versionId,
        filterText,
    }
}

/**
 * Dohledání konkrétního stromu.
 * {Object} state root store
 * {string} area jaký strom
 * {int} versionId verze AP
 */
function getFaTreeForFa(state, area, versionId) {
    var index = indexById(state.arrRegion.fas, versionId, "versionId");
    if (index != null) {
        var fa = state.arrRegion.fas[index];
        var faTree = getFaTree(fa, area);

        return faTree;
    } else {
        return null;
    }
}

/**
 * Změna aktuálně vybrané položky z možných výsledků hledání.
 * {func} dispatch dispatch
 * {string} area jaký strom
 * {Object} faTree store stromu
 * {int} newIndex nový vybraný index ve výsledcích hledání
 */
function changeCurrentIndex(dispatch, area, faTree, newIndex) {
    if (newIndex != faTree.filterCurrentIndex) {
        var nodeId = faTree.searchedIds[newIndex];
        var nodeParent = faTree.searchedParents[nodeId];
        switch (area) {
            case types.FA_TREE_AREA_MAIN:
                dispatch(faSelectSubNode(nodeId, nodeParent, false, newIndex, true));
            case types.FA_TREE_AREA_MOVEMENTS_LEFT:
                dispatch(faTreeSelectNode(area, nodeId, false, false, newIndex))
            case types.FA_TREE_AREA_MOVEMENTS_RIGHT:
                dispatch(faTreeSelectNode(area, nodeId, false, false, newIndex))
        }
    }
}

/**
 * Přesun na další položku vy výsledcích hledání.
 * {string} area jaký strom
 * {int} versionId verze AP
 */
export function faTreeFulltextNextItem(area, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var faTree = getFaTreeForFa(state, area, versionId);

        if (faTree && faTree.searchedIds.length > 0) {
            var newIndex;
            if (faTree.filterCurrentIndex == -1) {
                newIndex = 0;
            } else {
                newIndex = Math.min(faTree.filterCurrentIndex + 1, faTree.searchedIds.length - 1);
            }
            changeCurrentIndex(dispatch, area, faTree, newIndex);
        }
    }
}

/**
 * Přesun na předchozí položku vy výsledcích hledání.
 * {string} area jaký strom
 * {int} versionId verze AP
 */
export function faTreeFulltextPrevItem(area, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var faTree = getFaTreeForFa(state, area, versionId);

        if (faTree && faTree.searchedIds.length > 0) {
            var newIndex;
            if (faTree.filterCurrentIndex == -1) {
                newIndex = 0;
            } else {
                newIndex = Math.max(faTree.filterCurrentIndex - 1, 0);
            }
            changeCurrentIndex(dispatch, area, faTree, newIndex);
        }
    }
}

/**
 * Akce fulltextového hledání ve stromu. Text, podle kterého se hledá, je brán ze store konkrétního stromu.
 * {string} area jaký strom
 * {int} versionId verze AP
 */
export function faTreeFulltextSearch(area, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var faTree = getFaTreeForFa(state, area, versionId);
        if (faTree) {
            if (faTree.filterText.length > 0) {
                var searchParentNodeId
                // V případě stromu, který má multi select hledáme v celém stromu
                // V případě stromu, který má single select hledáme POD vybranou položkou
                if (faTree.multipleSelection) { // hledáme v celém stromu
                    searchParentNodeId = null
                } else {    // hledáme pod vybranou položkou, pokud nějaká je
                    if (faTree.selectedId !== null) {   // vybraná položka, hledáme pod ní
                        searchParentNodeId = faTree.selectedId
                    } else {    // nice není vybráno, hledáme v celém stromě
                        searchParentNodeId = null
                    }
                }

                return WebApi.findInFaTree(versionId, searchParentNodeId, faTree.filterText, 'SUBTREE')
                    .then(json => {
                        dispatch(faTreeFulltextResult(area, versionId, faTree.filterText, json, false))
                        if (json.length > 0) {
                            var newFaTree = getFaTreeForFa(getState(), area, versionId)
                            changeCurrentIndex(dispatch, area, newFaTree, 0);
                        }
                    });
            } else {
                dispatch(faTreeFulltextResult(area, versionId, faTree.filterText, [], true))
            }
        }
    }
}

/**
 * Výsledek hledání ve stromu.
 * {string} area jaký strom
 * {int} versionId verze AP
 * {string} filterText pro jaký hledaný text platí výsledky
 * {Arraz} searchedData seznam nalezených node
 * {boolean} clearFilter jedná se o akci, která má pouze vymazat aktuální filtr?
 */
function faTreeFulltextResult(area, versionId, filterText, searchedData, clearFilter) {
    return {
        type: types.FA_FA_TREE_FULLTEXT_RESULT,
        area,
        versionId,
        filterText,
        searchedData,
        clearFilter
    }
}

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeNodeExpand(area, node) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = getFaTree(activeFa, area);

        dispatch(_faTreeNodeExpand(area, node, true))

        var versionId = activeFa.versionId;
        var nodeId = node.id;
        var expandedIds = {...faTree.expandedIds, [nodeId]: true}
        return WebApi.getFaTree(versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(area, versionId, nodeId, expandedIds, [], json)));
    }
}

/**
 * Nastavení focusu pro uzel ve stromu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeFocusNode(area, node) {
    return {
        type: types.FA_FA_TREE_FOCUS_NODE,
        area,
        node,
    }
}

/**
 * Zabalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeNodeCollapse(area, node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE_NODE,
        area,
        node,
    }
}

/**
 * Získání store konkrétního store stromu na danou FA a oblast stromu.
 * {Object} fa store fa
 * {string} area o jaký strom se jedná
 */
function getFaTree(fa, area) {
    switch (area) {
        case types.FA_TREE_AREA_MAIN:
            return fa.faTree;
        case types.FA_TREE_AREA_MOVEMENTS_LEFT:
            return fa.faTreeMovementsLeft;
        case types.FA_TREE_AREA_MOVEMENTS_RIGHT:
            return fa.faTreeMovementsRight;
    }
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetchIfNeeded(area, versionId, expandedIds, selectedId) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = getFaTree(activeFa, area);

        var fetch = false;

        var includeIds = [];
        if (selectedId != null && typeof selectedId !== 'undefined') {
            includeIds.push(selectedId);

            var isInView = indexById(faTree.nodes, selectedId);
            if (isInView == null) {
                if (!faTree.fetchingIncludeIds[selectedId]) {
                    fetch = true;
                }
            }
        }

        if (!faTree.fetched && !faTree.isFetching) {
            fetch = true;
        }

        if (faTree.dirty && !faTree.isFetching) {
            fetch = true;
        }

        if (fetch) {
            return dispatch(faTreeFetch(area, versionId, null, expandedIds, includeIds));
        }
    }
}

/**
 * Nové načtení dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} pokud je uvedeno, obsahuje id node, pro který se mají vracet data, pokud není uveden, vrací se celý strom
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetch(area, versionId, nodeId, expandedIds, includeIds=[]) {
    return dispatch => {
        dispatch(faTreeRequest(area, versionId, nodeId, expandedIds, includeIds))
        return WebApi.getFaTree(versionId, nodeId, expandedIds, includeIds)
            .then(json => dispatch(faTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 * @param {Object} json objekt s daty
 */
export function faTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json) {
    return {
        type: types.FA_FA_TREE_RECEIVE,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
        nodes: json.nodes,
        expandedIdsExtension: json.expandedIdsExtension,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeRequest(area, versionId, nodeId, expandedIds, includeIds) {
    return {
        type: types.FA_FA_TREE_REQUEST,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
    }
}
