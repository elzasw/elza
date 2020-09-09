/**
 * Akce pro záložky uzlů JP.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes';
import {fundSelectSubNode, fundSelectSubNodeInt, increaseMultipleNodesVersions} from 'actions/arr/node.jsx';

export function isNodesAction(action) {
    switch (action.type) {
        case types.FUND_FUND_CLOSE_NODE_TAB:
        case types.FUND_FUND_SELECT_NODE_TAB:
            return true;
        default:
            return false;
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
        if (nodeTab.selectedSubNodeId != null) {
            // musíme poslat akci vybrání subnode (aby se řádek vybral např. ve stromu)
            dispatch(fundSelectSubNodeInt(versionId, nodeTab.selectedSubNodeId, nodeTab, false, null, true));
        }
    };
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
    };
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
        if (activeFund.nodes.activeIndex === index) {
            // zavíráme aktuálně vybranou
            wasSelected = true;
        }
        dispatch(_fundCloseNodeTab(versionId, nodeId, routingKey, index));
        var newState = getState();
        var newActiveFund = newState.arrRegion.funds[newState.arrRegion.activeIndex];
        if (wasSelected) {
            if (newActiveFund.nodes.nodes.length > 0) {
                // bude vybraná nějaká jiná, protože ještě nějaké záložky existují
                const node = newActiveFund.nodes.nodes[newActiveFund.nodes.activeIndex];
                dispatch(fundSelectNodeTab(versionId, node.id, node.routingKey, newActiveFund.nodes.activeIndex));
            } else {
                // není žádná záložka
                dispatch(fundSelectSubNodeInt(newActiveFund.versionId, null, null, false, null, false));
            }
        }
    };
}

/**
 * Funkce přesunu uzlů. Všechny funkce musí vracet Promise.
 */
const moveFunctions = {
    BEFORE: WebApi.moveNodesBefore,
    AFTER: WebApi.moveNodesAfter,
    UNDER: WebApi.moveNodesUnder,
};

/**
 * Funkce spouštějící a ukončující operaci přesunu.
 * @param {string} direction - směr přesunu uzlu
 * @param {int} versionId - verze AS
 * @param {Array} nodes - seznam uzlů pro akci
 * @param {Object} nodesParent - nadřazený uzel k nodes
 * @param {Object} dest - cílový uzel, kterého se akce týká
 * @param {Object} destParent - nadřazený uzel pro dest
 */
export function moveNodes(direction, versionId, nodes, nodesParent, dest, destParent) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
        var nodeTab = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        var nodesToUpdate = [...nodes];
        nodesToUpdate.push(nodesParent);
        var nextNodeParent;
        if (direction === 'UNDER') {
            nextNodeParent = dest;
        } else {
            nextNodeParent = destParent;
        }
        nodesToUpdate.push(nextNodeParent);
        dispatch(increaseMultipleNodesVersions(versionId, nodesToUpdate));
        dispatch(fundMoveStart(versionId));
        return moveFunctions[direction](versionId, nodes, nodesParent, dest, destParent).then(() => {
            dispatch(fundMoveFinish(versionId));
            dispatch(fundSelectSubNode(versionId, nodeTab.selectedSubNodeId, nextNodeParent));
        });
    };
}

/**
 * Akce zavolána při začátku přesunu
 * @param {int} versionId - verze AS určující pro, který AS se akce spustí
 */
function fundMoveFinish(versionId) {
    return {
        type: types.FUND_NODES_MOVE_STOP,
        versionId: versionId,
    };
}

/**
 * Akce zavolána po skončení přesunu
 * @param {int} versionId - verze AS určující pro, který AS se akce zastaví
 */
function fundMoveStart(versionId) {
    return {
        type: types.FUND_NODES_MOVE_START,
        versionId: versionId,
    };
}
