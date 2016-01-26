/**
 * Akce pro záložky uzlů JP.
 */

import {WebApi} from 'actions'
import {indexById} from 'stores/app/utils.jsx'
import {faExtendedView} from './fa'
import * as types from 'actions/constants/actionTypes';

/**
 * Změna vybrané záložky JP.
 * Pokud má daná záložka vybraný podřízený JP, je poslána nová akce s vybrání podřízené JP {@link faSelectSubNode}.
 * @param {int} index index vybrané záložky
 */
export function faSelectNodeTab(index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var nodeTab = activeFa.nodes.nodes[index];
        dispatch({
            type: types.FA_FA_SELECT_NODE_TAB,
            index,
        });
        if (nodeTab.selectedSubNodeId != null) {    // musíme poslat akci vybrání subnode (aby se řádek vybral např. ve stromu)
            dispatch(faSelectSubNodeInt(nodeTab.selectedSubNodeId, nodeTab, false));
        }
    }
}

/**
 * Zavření záložky JP.
 * @param index {int} index záložky
 */
export function _faCloseNodeTab(index) {
    return {
        type: types.FA_FA_CLOSE_NODE_TAB,
        index
    }
}

/**
 * Zavření záložky JP.
 * @param index {int} index záložky
 */
export function faCloseNodeTab(index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var wasSelected = false;
        if (activeFa.nodes.activeIndex == index) {  // zavíráme aktuálně vybranou
            wasSelected = true;
        }
        dispatch(_faCloseNodeTab(index));
        var newState = getState();
        var newActiveFa = newState.arrRegion.fas[newState.arrRegion.activeIndex];
        if (wasSelected) { 
            if (newActiveFa.nodes.nodes.length > 0) {    // je vybraná nějaká jiná, protože ještě nějaké záložky existují
                dispatch(faSelectNodeTab(newActiveFa.nodes.activeIndex));
            } else {    // není žádná záložka
                dispatch(faSelectSubNodeInt(null, null, false));
            }
        }
    }
}

/**
 * Vybrání podřízené JP pod záložkou JP (vybrání JP pro formulář).
 * @param {String} subNodeId id JP pro vybrání
 * @param {Object} subNodeParentNode nadřazený JP pro vybíranou JP, předáváno kvůli případnému otevření nové záložky, pokud neexistuje
 * @param {boolean} openNewTab má se otevřít nová záložka? Pokud je false, bude použita existující  aktuálně vybraná, pokud žádná neexistuje, bude nová vytvořena
 */
export function faSelectSubNodeInt(subNodeId, subNodeParentNode, openNewTab=false) {
    return {
        type: types.FA_FA_SELECT_SUBNODE,
        area: types.FA_TREE_AREA_MAIN,
        subNodeId,
        subNodeParentNode,
        openNewTab,
    }
}

export function faSelectSubNode(subNodeId, subNodeParentNode, openNewTab=false) {
    return (dispatch, getState) => {
        dispatch(faExtendedView(false));
        dispatch(faSelectSubNodeInt(subNodeId, subNodeParentNode, openNewTab));
    }
}

export function faSubNodesNext() {
    return {
        type: types.FA_FA_SUBNODES_NEXT,
    }
}

export function faSubNodesPrev() {
    return {
        type: types.FA_FA_SUBNODES_PREV,
    }
}

export function faSubNodesNextPage() {
    return {
        type: types.FA_FA_SUBNODES_NEXT_PAGE,
    }
}

export function faSubNodesPrevPage() {
    return {
        type: types.FA_FA_SUBNODES_PREV_PAGE,
    }
}

export function moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesUnder(versionId, nodes, nodesParent, dest, destParent);
    }
}

export function moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesBefore(versionId, nodes, nodesParent, dest, destParent);
    }
}

export function moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesAfter(versionId, nodes, nodesParent, dest, destParent);
    }
}