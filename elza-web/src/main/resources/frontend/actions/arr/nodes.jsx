/**
 * Akce pro záložky uzlů JP.
 */

import {WebApi} from 'actions/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {fundExtendedView} from './fund.jsx'
import * as types from 'actions/constants/ActionTypes.js';
import {developerNodeScenariosDirty} from 'actions/global/developer.jsx';
import {findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx'

export function isNodesAction(action) {
    switch (action.type) {
        case types.FUND_FUND_CLOSE_NODE_TAB:
        case types.FUND_FUND_SELECT_NODE_TAB:
            return true
        default:
            return false
    }
}

/**
 * Změna vybrané záložky JP.
 * Pokud má daná záložka vybraný podřízený JP, je poslána nová akce s vybrání podřízené JP {@link fundSelectSubNode}.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @param {int} index index vybrané záložky
 */
export function fundSelectNodeTab(versionId, nodeId, routingKey, index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
        var nodeTab = activeFund.nodes.nodes[index];
        dispatch({
            type: types.FUND_FUND_SELECT_NODE_TAB,
            versionId,
            nodeId,
            routingKey,
            index,
        });
        if (nodeTab.selectedSubNodeId != null) {    // musíme poslat akci vybrání subnode (aby se řádek vybral např. ve stromu)
            dispatch(fundSelectSubNodeInt(versionId, nodeTab.selectedSubNodeId, nodeTab, false, null, true));
        }
    }
}

/**
 * Zavření záložky JP.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @param index {int} index záložky
 */
function _fundCloseNodeTab(versionId, nodeId, routingKey, index) {
    return {
        type: types.FUND_FUND_CLOSE_NODE_TAB,
        versionId,
        nodeId,
        routingKey,
        index,
    }
}

/**
 * Zavření záložky JP.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @param index {int} index záložky
 */
export function fundCloseNodeTab(versionId, nodeId, routingKey, index) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
        var wasSelected = false;
        if (activeFund.nodes.activeIndex == index) {  // zavíráme aktuálně vybranou
            wasSelected = true;
        }
        dispatch(_fundCloseNodeTab(versionId, nodeId, routingKey, index));
        var newState = getState();
        var newActiveFund = newState.arrRegion.funds[newState.arrRegion.activeIndex];
        if (wasSelected) { 
            if (newActiveFund.nodes.nodes.length > 0) {    // bude vybraná nějaká jiná, protože ještě nějaké záložky existují
                const node = newActiveFund.nodes.nodes[newActiveFund.nodes.activeIndex]
                dispatch(fundSelectNodeTab(versionId, node.id, node.routingKey, newActiveFund.nodes.activeIndex));
            } else {    // není žádná záložka
                dispatch(fundSelectSubNodeInt(newActiveFund.versionId, null, null, false, null, false));
            }
        }
    }
}

/**
 * Vybrání podřízené JP pod záložkou JP (vybrání JP pro formulář).
 * @param {int} versionId verze AS
 * @param {String} subNodeId id JP pro vybrání
 * @param {Object} subNodeParentNode nadřazený JP pro vybíranou JP, předáváno kvůli případnému otevření nové záložky, pokud neexistuje
 * @param {boolean} openNewTab má se otevřít nová záložka? Pokud je false, bude použita existující  aktuálně vybraná, pokud žádná neexistuje, bude nová vytvořena
 */
export function fundSelectSubNodeInt(versionId, subNodeId, subNodeParentNode, openNewTab=false, newFilterCurrentIndex = null, ensureItemVisible=false) {
    return {
        type: types.FUND_FUND_SELECT_SUBNODE,
        area: types.FUND_TREE_AREA_MAIN,
        versionId,
        subNodeId,
        subNodeParentNode,
        openNewTab,
        newFilterCurrentIndex,
        ensureItemVisible
    }
}

/**
 * Akce vybrání záložky NODE v Accordion v aktuální záložce NODE pod aktuální vybranou záložkou AS. V případě, že neexsituje aktuální záložka NODE
 * je vytvořena nová na základě parametru subNodeParentNode, který bude reprezentovat záložku.
 * @param {int} versionId verze AS
 * @param {int} subNodeId id node, který má být vybrán v Accordion
 * @param {Object} subNodeParentNode nadřazený node k subNodeId
 * @param {boolean} openNewTab pokud je true, je vždy vytvářena nová záložka. pokud je false, je nová záložka vytvářena pouze pokud žádná není
 * @param {int} newFilterCurrentIndex nový index ve výsledcích hledání ve stromu, pokud daná akce je vyvolána akcí skuku na jinou vyhledanou položku vy výsledcích hledání ve stromu
 * @param {boolean} ensureItemVisible true, pokud má být daná položka vidět - má se odscrolovat
 */
export function fundSelectSubNode(versionId, subNodeId, subNodeParentNode, openNewTab=false, newFilterCurrentIndex = null, ensureItemVisible=false) {
    return (dispatch, getState) => {
        dispatch(fundExtendedView(false));
        dispatch(fundSelectSubNodeInt(versionId, subNodeId, subNodeParentNode, openNewTab, newFilterCurrentIndex, ensureItemVisible));
        let state = getState();
        dispatch(developerNodeScenariosDirty(subNodeId, subNodeParentNode.routingKey, state.arrRegion.funds[state.arrRegion.activeIndex].versionId));
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * @param {int} versionId verze AS
 * @param {Array} nodes seznam uzlů pro akci
 * @param {Object} nodesParent nadřazený uzel k nodes
 * @param {Object} dest cílový uzel, kterého se akce týká
 * @param {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesUnder(versionId, nodes, nodesParent, dest, destParent);
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * @param {int} versionId verze AS
 * @param {Array} nodes seznam uzlů pro akci
 * @param {Object} nodesParent nadřazený uzel k nodes
 * @param {Object} dest cílový uzel, kterého se akce týká
 * @param {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesBefore(versionId, nodes, nodesParent, dest, destParent);
    }
}

/**
 * Akce přesunu uzlů ve stromu.
 * @param {int} versionId verze AS
 * @param {Array} nodes seznam uzlů pro akci
 * @param {Object} nodesParent nadřazený uzel k nodes
 * @param {Object} dest cílový uzel, kterého se akce týká
 * @param {Object} destParent nadřazený uzel pro dest
 */
export function moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        WebApi.moveNodesAfter(versionId, nodes, nodesParent, dest, destParent);
    }
}