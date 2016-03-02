/**
 * Akce pro záložky uzlů JP.
 */

import {WebApi} from 'actions'
import {indexById} from 'stores/app/utils.jsx'
import {faExtendedView} from './fa'
import * as types from 'actions/constants/ActionTypes';
import {developerNodeScenariosDirty} from 'actions/global/developer';

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
            dispatch(faSelectSubNodeInt(nodeTab.selectedSubNodeId, nodeTab, false, null, true));
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
                dispatch(faSelectSubNodeInt(null, null, false, null, false));
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
export function faSelectSubNodeInt(subNodeId, subNodeParentNode, openNewTab=false, newFilterCurrentIndex = null, ensureItemVisible=false) {
    return {
        type: types.FA_FA_SELECT_SUBNODE,
        area: types.FA_TREE_AREA_MAIN,
        subNodeId,
        subNodeParentNode,
        openNewTab,
        newFilterCurrentIndex,
        ensureItemVisible
    }
}

/**
 * Akce vybrání záložky NODE v Accordion v aktuální záložce NODE pod aktuální vybranou záložkou AP. V případě, že neexsituje aktuální záložka NODE
 * je vytvořena nová na základě parametru subNodeParentNode, který bude reprezentovat záložku.
 * {int} subNodeId id node, který má být vzbrán v Accordion
 * {Object} subNodeParentNode nadřazený node k subNodeId
 * {boolean} openNewTab pokud je true, je vždy vytvářena nová záložka. pokud je false, je nová záložka vytvářena pouze pokud žádná není
 * {int} newFilterCurrentIndex nový index ve výsledcích hledání ve stromu, pokud daná akce je vyvolána akcí skuku na jinou vyhledanou položku vy výsledcích hledání ve stromu
 * {boolean} ensureItemVisible true, pokud má být daná položka vidět - má se odscrolovat
 */
export function faSelectSubNode(subNodeId, subNodeParentNode, openNewTab=false, newFilterCurrentIndex = null, ensureItemVisible=false) {
    return (dispatch, getState) => {
        dispatch(faExtendedView(false));
        dispatch(faSelectSubNodeInt(subNodeId, subNodeParentNode, openNewTab, newFilterCurrentIndex, ensureItemVisible));
        let state = getState();
        dispatch(developerNodeScenariosDirty(subNodeId, subNodeParentNode.nodeKey, state.arrRegion.fas[state.arrRegion.activeIndex].versionId));
    }
}

/**
 * Stránkování v Accordion - další část.
 */
export function faSubNodesNext() {
    return {
        type: types.FA_FA_SUBNODES_NEXT,
    }
}

/**
 * Stránkování v Accordion - předchozí část.
 */
export function faSubNodesPrev() {
    return {
        type: types.FA_FA_SUBNODES_PREV,
    }
}

/**
 * Stránkování v Accordion - další stránka.
 */
export function faSubNodesNextPage() {
    return (dispatch, getState) => {
        let state = getState();
        let activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        let node = activeFa.nodes.nodes[activeFa.nodes.activeIndex];
        let viewIndex = node.viewStartIndex;
        let index = indexById(node.childNodes, node.selectedSubNodeId);
        dispatch(_faSubNodesNextPage());

        if (index != null) {
            let newState = getState();
            let newActiveFa = newState.arrRegion.fas[newState.arrRegion.activeIndex];
            let newNode = newActiveFa.nodes.nodes[newActiveFa.nodes.activeIndex];
            let newViewIndex = newNode.viewStartIndex;
            let newIndex = newViewIndex - viewIndex + index;
            let count = newNode.childNodes.length;
            let subNodeId = newIndex < count ? newNode.childNodes[newIndex].id : newNode.childNodes[count - 1].id;
            let subNodeParentNode = newNode;
            dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
        }
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 */
export function _faSubNodesNextPage() {
    return {
        type: types.FA_FA_SUBNODES_NEXT_PAGE,
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 */
export function faSubNodesPrevPage() {
    return (dispatch, getState) => {
        let state = getState();
        let activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        let node = activeFa.nodes.nodes[activeFa.nodes.activeIndex];
        let viewIndex = node.viewStartIndex;
        let index = indexById(node.childNodes, node.selectedSubNodeId);
        dispatch(_faSubNodesPrevPage());

        if (index != null) {
            let newState = getState();
            let newActiveFa = newState.arrRegion.fas[newState.arrRegion.activeIndex];
            let newNode = newActiveFa.nodes.nodes[newActiveFa.nodes.activeIndex];
            let newViewIndex = newNode.viewStartIndex;
            let newIndex = newViewIndex - viewIndex + index;
            let subNodeId = newIndex < 0 ? newNode.childNodes[0].id : newNode.childNodes[newIndex].id;
            let subNodeParentNode = newNode;
            dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
        }
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 */
export function _faSubNodesPrevPage() {
    return {
        type: types.FA_FA_SUBNODES_PREV_PAGE,
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * {int} versionId verze AP
 * {Array} nodes seznam uzlů pro akci
 * {Object} nodesParent nadřazený uzel k nodes
 * {Object} dest cílový uzel, kterého se akce týká
 * {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesUnder(versionId, nodes, nodesParent, dest, destParent);
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * {int} versionId verze AP
 * {Array} nodes seznam uzlů pro akci
 * {Object} nodesParent nadřazený uzel k nodes
 * {Object} dest cílový uzel, kterého se akce týká
 * {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesBefore(versionId, nodes, nodesParent, dest, destParent);
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * {int} versionId verze AP
 * {Array} nodes seznam uzlů pro akci
 * {Object} nodesParent nadřazený uzel k nodes
 * {Object} dest cílový uzel, kterého se akce týká
 * {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesAfter(versionId, nodes, nodesParent, dest, destParent);
    }
}