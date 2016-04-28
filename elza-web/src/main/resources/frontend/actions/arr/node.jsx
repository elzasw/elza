/**
 * Akce pro vybranou záložku NODE pod konkrétní vybranou záložkou AS.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {fundSelectSubNode} from 'actions/arr/nodes.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {isFundRootId} from 'components/arr/ArrUtils.jsx'

export function isNodeAction(action) {
    switch (action.type) {
        case types.FUND_FUND_SUBNODES_FULLTEXT_RESULT:
        case types.FUND_FUND_SUBNODES_NEXT:
        case types.FUND_FUND_SUBNODES_PREV:
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
            return true
        default:
            return false
    }
}

/**
 * Fetch dat pro otevřené záložky NODE, pokud je potřeba.
 * {int} versionId verze AS
 */
export function nodesFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var index = indexById(state.arrRegion.funds, versionId, "versionId");
        if (index !== null) {
            var fund = state.arrRegion.funds[index]
            var nodeIds = [];
            fund.nodes.nodes.forEach(node => {
                if (node.dirty && !node.isFetching) {
                    if (isFundRootId(node.id)) {  // virtuální představující AS, je nutné aktualizovat z AS
                        // bude se aktualizovat až s načtením a obnovením AS
                    } else {
                        nodeIds.push(node.id);
                    }
                }
            })

            if (nodeIds.length > 0) {
                dispatch(nodesRequest(versionId, nodeIds));

                WebApi.getNodes(versionId, nodeIds)
                    .then(json => {
                        dispatch(nodesReceive(versionId, json));
                    })
            }
        }
    }
}

/**
 * Akce vyžádání dat - informace do store.
 * {int} versionId verze AS
 * {Array} nodeIds seznam id node, pro které byla data vyžádána
 */
export function nodesRequest(versionId, nodeIds) {
    var nodeMap = {}
    nodeIds.forEach(id => {
        nodeMap[id] = true
    })

    return {
        type: types.FUND_NODES_REQUEST,
        versionId,
        nodeMap
    }
}

/**
 * Akce přijetí dat - promítnutí do store.
 * {int} versionId verze AS
 * {Array} nodes seznam node
 */
export function nodesReceive(versionId, nodes) {
    var nodeMap = {}
    nodes.forEach(node => {
        nodeMap[node.id] = node
    })

    return {
        type: types.FUND_NODES_RECEIVE,
        versionId,
        nodes,
        nodeMap
    }
}

/**
 * Výsledek hledání v seznamu sourozenců - Accordion.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 * {Array} nodeIds seznam vyfiltrovaných node
 */
export function fundNodeSubNodeFulltextResult(versionId, nodeId, nodeKey, nodeIds) {
    return {
        type: types.FUND_FUND_SUBNODES_FULLTEXT_RESULT,
        versionId,
        nodeId,
        nodeKey,
        nodeIds
    }
}

/**
 * Hledání v seznamu sourozenců - Accordion pro konkrétní zobrazení.
 * {string} filterText podle jakého řetězce se má vyhledávat
 */
export function fundNodeSubNodeFulltextSearch(filterText) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
        var activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];

        dispatch({
            type: types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH,
            filterText
        })
        if (filterText !== '') {
            WebApi.findInFundTree(activeFund.versionId, activeNode.id, filterText, 'ONE_LEVEL')
                .then(json => {
                    dispatch(fundNodeSubNodeFulltextResult(activeFund.versionId, activeNode.id, activeNode.nodeKey, json));
                    if (json.length > 0) {
                        dispatch(activeFund.versionId, fundSelectSubNode(json[0].nodeId, json[0].parent, false, null, true));
                    }
                })
        } else {
            dispatch(fundNodeSubNodeFulltextResult(activeFund.versionId, activeNode.id, activeNode.nodeKey, []));
        }
    }
}

/**
 * Přidání uzlu před, za na konec a pod.
 * @param {Object} indexNode uzel, pro který je volána akce - před, za a pod který se akce volá
 * @param {Object} parentNode nadřazený uzel k indexNode, pokud je přidáváno jako 'CHILD', je stejný jako indexNode
 * @param {int} versionId verze AS
 * @param {string} direction směr přidání, řetězec 'BEFORE', 'AFTER' nebo 'CHILD'
 * @param {Array} descItemCopyTypes seznam id atributů, pro které se mají zkopírovat hodnoty z bezprostředně předcházejícího uzlu, který je před aktuálně přidávaným
 * @param {string} scenarioName název scénáře, který má být použit
 */
export function addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes = null, scenarioName = null) {
    return (dispatch) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version
        };
        indexNode = {
            id: indexNode.id,
            lastUpdate: indexNode.lastUpdate,
            version: indexNode.version
        };
        return WebApi.addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes, scenarioName).then((json) => {
            dispatch(fundNodeChangeAdd(versionId, json.node, indexNode, json.parentNode, direction));
            dispatch(fundSelectSubNode(versionId, json.node.id, json.parentNode));
        });
    }
}

/**
 * Akce smazání uzlu.
 * @param {Object} node uzel, který se má smazat
 * @param {Object} parentNode nadřazený uzel k mazanému
 * @param {int} versionId vezer AS
 */
export function deleteNode(node, parentNode, versionId) {
    return (dispatch) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version
        };
        node = {
            id: node.id,
            lastUpdate: node.lastUpdate,
            version: node.version
        };
        return WebApi.deleteNode(node, parentNode, versionId).then((json) => {
            dispatch(fundNodeChangeDelete(versionId, json.node, json.parentNode));
        });
    }
}

/**
 * Informační akce o přidání uzlu.
 * @param {int} versionId verze AS
 * @param {Object} newNode nově přidaný uzel
 * @param {Object} indexNode uzel, pro který je volána akce - před, za a pod který se akce volá
 * @param {Object} parentNode nadřazený uzel k indexNode, pokud je přidáváno jako 'CHILD', je stejný jako indexNode
 * @param {string} direction 'BEFORE', 'AFTER' nebo 'CHILD'
 */
function fundNodeChangeAdd(versionId, newNode, indexNode, parentNode, direction) {
    return {
        newNode,
        indexNode,
        parentNode,
        direction,
        action: 'ADD',
        type: types.FUND_NODE_CHANGE,
        versionId
    }
}

/**
 * Informační akce o smazání uzlu.
 * @param {int} versionId verze AS
 * @param {Object} node smazaný uzel
 * @param {Object} parentNode nadřazený uzel k mazanému
 */
function fundNodeChangeDelete(versionId, node, parentNode) {
    return {
        node,
        parentNode,
        action: 'DELETE',
        type: types.FUND_NODE_CHANGE,
        versionId
    }
}

/**
 * Stránkování v Accordion - další část.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
export function fundSubNodesNext(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_FUND_SUBNODES_NEXT,
        versionId,
        nodeId,
        nodeKey,
    }
}

/**
 * Stránkování v Accordion - předchozí část.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
export function fundSubNodesPrev(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_FUND_SUBNODES_PREV,
        versionId,
        nodeId,
        nodeKey,
    }
}

/**
 * Stránkování v Accordion - další stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
export function fundSubNodesNextPage(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        const r = findByNodeKeyInGlobalState(getState(), versionId, nodeKey)
        if (r) {
            let activeFund = r.fund
            let node = r.node
            let viewIndex = node.viewStartIndex;
            let index = indexById(node.childNodes, node.selectedSubNodeId);
            dispatch(_fundSubNodesNextPage(versionId, nodeId, nodeKey));

            if (index != null) {
                const rnew = findByNodeKeyInGlobalState(getState(), versionId, nodeKey)
                let newActiveFund = rnew.fund
                let newNode = rnew.node
                let newViewIndex = newNode.viewStartIndex;
                let newIndex = newViewIndex - viewIndex + index;
                let count = newNode.childNodes.length;
                let subNodeId = newIndex < count ? newNode.childNodes[newIndex].id : newNode.childNodes[count - 1].id;
                let subNodeParentNode = newNode;
                dispatch(fundSelectSubNode(newActiveFund.versionId, subNodeId, subNodeParentNode, false, null, true));
            }
        }
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
export function _fundSubNodesNextPage(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_FUND_SUBNODES_NEXT_PAGE,
        versionId,
        nodeId,
        nodeKey,
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
export function fundSubNodesPrevPage(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        const r = findByNodeKeyInGlobalState(getState(), versionId, nodeKey)
        let activeFund = r.fund
        let node = r.node
        let viewIndex = node.viewStartIndex;
        let index = indexById(node.childNodes, node.selectedSubNodeId);
        dispatch(_fundSubNodesPrevPage(versionId, nodeId, nodeKey));

        if (index != null) {
            const rnew = findByNodeKeyInGlobalState(getState(), versionId, nodeKey)
            let newActiveFund = rnew.fund
            let newNode = rnew.node
            let newViewIndex = newNode.viewStartIndex;
            let newIndex = newViewIndex - viewIndex + index;
            let subNodeId = newIndex < 0 ? newNode.childNodes[0].id : newNode.childNodes[newIndex].id;
            let subNodeParentNode = newNode;
            dispatch(fundSelectSubNode(newActiveFund.versionId, subNodeId, subNodeParentNode, false, null, true));
        }
    }
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 */
function _fundSubNodesPrevPage(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_FUND_SUBNODES_PREV_PAGE,
        versionId,
        nodeId,
        nodeKey,
    }
}