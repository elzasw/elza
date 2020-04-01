/**
 * Akce pro vybranou záložku NODE pod konkrétní vybranou záložkou AS.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from './../../actions/constants/ActionTypes.js';
import {findByRoutingKeyInGlobalState, indexById} from 'stores/app/utils.jsx';
import {createFundRoot, isFundRootId} from 'components/arr/ArrUtils.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {fundExtendedView} from './fundExtended';
import {developerNodeScenariosDirty} from 'actions/global/developer.jsx';
import {fundNodeInfoReceive} from './nodeInfo';

export function isNodeAction(action) {
    switch (action.type) {
        case types.FUND_FUND_SUBNODES_FULLTEXT_RESULT:
        case types.FUND_FUND_SUBNODES_NEXT:
        case types.FUND_FUND_SUBNODES_PREV:
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
            return true;
        default:
            return false;
    }
}

/**
 * Vybrání podřízené JP pod záložkou JP (vybrání JP pro formulář).
 * @param {int} versionId verze AS
 * @param {String} subNodeId id JP pro vybrání
 * @param {Object} subNodeParentNode nadřazený JP pro vybíranou JP, předáváno kvůli případnému otevření nové záložky, pokud neexistuje
 * @param {boolean} openNewTab má se otevřít nová záložka? Pokud je false, bude použita existující  aktuálně vybraná, pokud žádná neexistuje, bude nová vytvořena
 */
export function fundSelectSubNodeInt(
    versionId,
    subNodeId,
    subNodeParentNode,
    openNewTab = false,
    newFilterCurrentIndex = null,
    ensureItemVisible = false,
    subNodeIndex,
) {
    return {
        type: types.FUND_FUND_SELECT_SUBNODE,
        area: types.FUND_TREE_AREA_MAIN,
        versionId,
        subNodeId,
        subNodeParentNode,
        openNewTab,
        newFilterCurrentIndex,
        ensureItemVisible,
        subNodeIndex,
    };
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
 * @param subNodeIndex
 */
export function fundSelectSubNode(
    versionId,
    subNodeId,
    subNodeParentNode,
    openNewTab = false,
    newFilterCurrentIndex = null,
    ensureItemVisible = false,
    subNodeIndex = null,
) {
    return (dispatch, getState) => {
        dispatch(fundExtendedView(false));

        let state = getState();

        // vyhledání indexu uzlu, nejprve se vyzkouší strom
        let nodes = state.arrRegion.funds[state.arrRegion.activeIndex].fundTree.nodes;
        if (!subNodeId && subNodeIndex !== null) {
            const index = indexById(nodes, subNodeParentNode.id) + subNodeIndex + 1;
            subNodeId = nodes[index].id;
        }
        let index = indexById(nodes, subNodeId);
        // pokud není ve stromu, tak se zkusí i akordeon - při přidání položky
        // je někdy dříve v akordeonu než-li ve stromu
        if (index === null) {
            const fund = state.arrRegion.funds[state.arrRegion.activeIndex];
            nodes = fund.nodes.nodes[fund.nodes.activeIndex].childNodes;
            index = indexById(nodes, subNodeId);

            subNodeIndex = index;
        } else {
            // korekce pozice v případě stromu o nadřazené
            let i = index;
            const node = nodes[index];
            for (; i >= 0; i--) {
                const n = nodes[i];
                if (n.depth < node.depth) {
                    break;
                }
            }
            subNodeIndex = index - i - 1;
        }

        dispatch(
            fundSelectSubNodeInt(
                versionId,
                subNodeId,
                subNodeParentNode,
                openNewTab,
                newFilterCurrentIndex,
                ensureItemVisible,
                subNodeIndex,
            ),
        );
        dispatch(
            developerNodeScenariosDirty(
                subNodeId,
                subNodeParentNode.routingKey,
                state.arrRegion.funds[state.arrRegion.activeIndex].versionId,
            ),
        );
    };
}

export function fundSelectSubNodeByNodeId(
    versionId,
    nodeId,
    openNewTab = false,
    newFilterCurrentIndex = null,
    ensureItemVisible = false,
    subNodeIndex = null,
) {
    return (dispatch, getState) => {
        const {arrRegion} = getState();
        const activeFund = arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : null;

        const getParentNodeByNodeId = (nodeId, fundTreeNodes) => {
            let index = indexById(fundTreeNodes, nodeId);
            const node = fundTreeNodes[index];
            while (--index >= 0) {
                if (fundTreeNodes[index].depth < node.depth) {
                    return fundTreeNodes[index];
                }
            }
            return null;
        };

        let parentNode = getParentNodeByNodeId(nodeId, activeFund.nodes.nodes);
        if (parentNode === null) {
            parentNode = createFundRoot(activeFund);
        }
        dispatch(
            fundSelectSubNode(
                versionId,
                nodeId,
                parentNode,
                openNewTab,
                newFilterCurrentIndex,
                ensureItemVisible,
                subNodeIndex,
            ),
        );
    };
}

/**
 * Fetch dat pro otevřené záložky NODE, pokud je potřeba.
 * {int} versionId verze AS
 */
export function nodesFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var index = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (index !== null) {
            var fund = state.arrRegion.funds[index];
            var nodeIds = [];
            fund.nodes.nodes.forEach(node => {
                if (node.dirty && !node.isFetching) {
                    if (isFundRootId(node.id)) {
                        // virtuální představující AS, je nutné aktualizovat z AS
                        // bude se aktualizovat až s načtením a obnovením AS
                    } else {
                        nodeIds.push(node.id);
                    }
                }
            });

            if (nodeIds.length > 0) {
                dispatch(nodesRequest(versionId, nodeIds));

                WebApi.getNodes(versionId, nodeIds).then(json => {
                    dispatch(nodesReceive(versionId, json));
                });
            }
        }
    };
}

/**
 * Akce vyžádání dat - informace do store.
 * {int} versionId verze AS
 * {Array} nodeIds seznam id node, pro které byla data vyžádána
 */
export function nodesRequest(versionId, nodeIds) {
    var nodeMap = {};
    nodeIds.forEach(id => {
        nodeMap[id] = true;
    });

    return {
        type: types.FUND_NODES_REQUEST,
        versionId,
        nodeMap,
    };
}

/**
 * Akce přijetí dat - promítnutí do store.
 * {int} versionId verze AS
 * {Array} nodes seznam node
 */
export function nodesReceive(versionId, nodes) {
    var nodeMap = {};
    nodes.forEach(node => {
        nodeMap[node.id] = node;
    });

    return {
        type: types.FUND_NODES_RECEIVE,
        versionId,
        nodes,
        nodeMap,
    };
}

/**
 * Výsledek hledání v seznamu sourozenců - Accordion.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * {Array} nodeIds seznam vyfiltrovaných node
 */
export function fundNodeSubNodeFulltextResult(versionId, nodeId, routingKey, nodeIds) {
    return {
        type: types.FUND_FUND_SUBNODES_FULLTEXT_RESULT,
        versionId,
        nodeId,
        routingKey,
        nodeIds,
    };
}

/**
 * Hledání v seznamu sourozenců - Accordion pro konkrétní zobrazení.
 * {string} filterText podle jakého řetězce se má vyhledávat
 */
export function fundNodeSubNodeFulltextSearch(filterText) {
    return (dispatch, getState) => {
        const state = getState();
        const activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
        const activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];

        dispatch({
            type: types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH,
            versionId: activeFund.versionId,
            nodeId: activeNode.id,
            routingKey: activeNode.routingKey,
            filterText,
        });

        let nodeId;
        if (activeNode.id != null && (typeof activeNode.id === 'string' || activeNode.id instanceof String)) {
            nodeId = null;
        } else {
            nodeId = activeNode.id;
        }

        // activeFund.versionId, nodeId, false, false, false, 0, activeNode.pageSize, filterText !== '' ? filterText : null, true)
        const nodeParam = {parentNodeId: nodeId, nodeIndex: 0};
        const resultParam = {
            formData: true,
            parents: false,
            children: false,
            siblingsFrom: 0,
            siblingsMaxCount: activeNode.pageSize,
            siblingsFilter: filterText !== '' ? filterText : null,
        };
        return WebApi.getNodeData(activeFund.versionId, nodeParam, resultParam).then(json => {
            dispatch(
                fundNodeInfoReceive(
                    activeFund.versionId,
                    nodeId,
                    activeNode.routingKey,
                    {
                        childNodes: json.siblings ? json.siblings : null,
                        nodeCount: json.nodeCount,
                        nodeIndex: json.nodeIndex,
                    },
                    true,
                ),
            );
            if (json.siblings && json.siblings.length > 0) {
                const node = json.siblings[0];
                dispatch(fundSelectSubNode(activeFund.versionId, node.id, activeNode));
            }
        });
    };
}

/**
 * Provedení umělého navýšení verze pro konkrétní node. Zvýšení probíhá o 1.
 * @param versionId verze AS
 * @param nodeId id node
 * @param nodeVersionId jaké id verze se má povýšit - pokud node již bude mít jinou verzi, nebude se zvyšovat
 */
export function increaseNodeVersion(versionId, nodeId, nodeVersionId) {
    return {
        type: types.FUND_NODE_INCREASE_VERSION,
        versionId,
        nodeId,
        nodeVersionId,
    };
}
/**
 * Provedení umělého navýšení verze pro více uzlů najednou
 * @param versionId verze AS
 * @param nodeArray pole objektů uzlů (pro zvýšení verze musí obsahovat id a version)
 */
export function increaseMultipleNodesVersions(versionId, nodeArray) {
    return dispatch => {
        for (var node in nodeArray) {
            node = nodeArray[node];
            if (node.id && node.version) {
                dispatch(increaseNodeVersion(versionId, node.id, node.version));
            }
        }
    };
}
/**
 * Přidání uzlu před, za na konec a pod.
 * @param {Object} indexNode uzel, pro který je volána akce - před, za a pod který se akce volá
 * @param {Object} parentNode nadřazený uzel k indexNode, pokud je přidáváno jako 'CHILD', je stejný jako indexNode
 * @param {int} versionId verze AS
 * @param {string} direction směr přidání, řetězec 'BEFORE', 'AFTER' nebo 'CHILD'
 * @param {Array} descItemCopyTypes seznam id atributů, pro které se mají zkopírovat hodnoty z bezprostředně předcházejícího uzlu, který je před aktuálně přidávaným
 * @param {string} scenarioName název scénáře, který má být použit
 * @param createItems
 * @param afterCreateCallback callback, který je volán po úspěšném založení, předpis: function (versionId, node, parentNode), node je nově založený node a parentNode je jeho aktualizovaný nadřazený node
 * @param emptyItemTypeIds seznam identifikátorů typů atributu, které budou přidány na detail JP po založení
 */
export function addNode(
    indexNode,
    parentNode,
    versionId,
    direction,
    descItemCopyTypes = null,
    scenarioName = null,
    createItems = null,
    afterCreateCallback = null,
    emptyItemTypeIds = null,
) {
    return (dispatch, getState) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version,
        };
        indexNode = {
            id: indexNode.id,
            lastUpdate: indexNode.lastUpdate,
            version: indexNode.version,
        };

        // Umělé zvednutí id verze node na klientovi na očekávané po operaci
        dispatch(increaseNodeVersion(versionId, parentNode.id, parentNode.version));

        // Reálné provedení operace
        return savingApiWrapper(
            dispatch,
            WebApi.addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes, scenarioName, createItems),
        ).then(json => {
            dispatch(fundNodeChangeAdd(versionId, json.node, indexNode, json.parentNode, direction));
            afterCreateCallback && afterCreateCallback(versionId, json.node, json.parentNode);

            const state = getState();
            const activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
            const activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];

            if (emptyItemTypeIds) {
                dispatch({
                    type: types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPES_ADD_TEMPLATE,
                    area: 'NODE',
                    versionId,
                    routingKey: activeNode.routingKey,
                    itemTypeIds: emptyItemTypeIds,
                });
            }
        });
    };
}

/**
 * Akce smazání uzlu.
 * @param {Object} node uzel, který se má smazat
 * @param {Object} parentNode nadřazený uzel k mazanému
 * @param {int} versionId verze AS
 * @param {func} afterDeleteCallback - callback, který je volán po úspěšném smazání,
 *     předpis: function (versionId, node, parentNode), node je smazaný node a parentNode je jeho aktualizovaný nadřazený node
 */
export function deleteNode(node, parentNode, versionId, afterDeleteCallback) {
    return dispatch => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version,
        };
        node = {
            id: node.id,
            lastUpdate: node.lastUpdate,
            version: node.version,
        };

        // Umělé zvednutí id verze node na klientovi na očekávané po operaci
        dispatch(increaseNodeVersion(versionId, parentNode.id, parentNode.version));

        // Reálné provedení operace
        return WebApi.deleteNode(node, parentNode, versionId).then(json => {
            afterDeleteCallback && afterDeleteCallback(versionId, json.node, json.parentNode);
            dispatch(fundNodeChangeDelete(versionId, json.node, json.parentNode));
        });
    };
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
        versionId,
    };
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
        versionId,
    };
}

/**
 * Načtení stránky akordeonu.
 *
 * @param getState   stav
 * @param versionId  verze AS
 * @param routingKey routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @param dispatch   dispatch
 * @param fceIndex   funkce pro výpočet indexu pro načtení
 */
const fundNodeSelect = function(getState, versionId, routingKey, dispatch, fceIndex) {
    const r = findByRoutingKeyInGlobalState(getState(), versionId, routingKey);
    if (r) {
        const node = r.node;
        let nodeId = node.selectedSubNodeId;
        if (!node.selectedSubNodeId && node.childNodes.length > 0) {
            nodeId = node.childNodes[0].id;
        }

        if (nodeId) {
            let index = fceIndex(node.viewStartIndex, node.pageSize / 2);
            index = index < 0 ? 0 : index;
            const nodeParam = {nodeId};
            const resultParam = {siblingsFrom: index, siblingsMaxCount: node.pageSize, siblingsFilter: node.filterText};
            WebApi.getNodeData(versionId, nodeParam, resultParam).then(json => {
                dispatch(
                    fundNodeInfoReceive(
                        versionId,
                        nodeId,
                        routingKey,
                        {
                            childNodes: json.siblings ? json.siblings : null,
                            nodeCount: json.nodeCount,
                            nodeIndex: json.nodeIndex,
                        },
                        true,
                    ),
                );
            });
        }
    }
};

/**
 * Stránkování v Accordion - další část.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodesNext(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        fundNodeSelect(getState, versionId, routingKey, dispatch, (a, b) => a + b);

        dispatch({
            type: types.FUND_FUND_SUBNODES_NEXT,
            versionId,
            nodeId,
            routingKey,
        });
    };
}

/**
 * Stránkování v Accordion - předchozí část.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodesPrev(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        fundNodeSelect(getState, versionId, routingKey, dispatch, (a, b) => a - b);

        dispatch({
            type: types.FUND_FUND_SUBNODES_PREV,
            versionId,
            nodeId,
            routingKey,
        });
    };
}

/**
 * Stránkování v Accordion - další stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodesNextPage(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        const state = getState();
        const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
        if (r) {
            const node = r.node;

            const nodes = state.arrRegion.funds[state.arrRegion.activeIndex].fundTree.nodes;
            const x = indexById(nodes, node.selectedSubNodeId);
            const xn = nodes[x];
            let subNodeIndex = x;
            for (let i = x; i >= 0; i--) {
                const n = nodes[i];
                if (n.depth === xn.depth) {
                    subNodeIndex--;
                } else {
                    if (n.depth < xn.depth) {
                        break;
                    }
                }
            }

            const index = x - subNodeIndex;
            dispatch(_fundSubNodesNextPage(versionId, nodeId, routingKey));

            if (index != null) {
                const rnew = findByRoutingKeyInGlobalState(getState(), versionId, routingKey);
                const newActiveFund = rnew.fund;
                const newNode = rnew.node;
                const newIndex = index + node.pageSize;
                const count = newNode.nodeCount;
                const subNodeId =
                    newIndex < count
                        ? nodes[subNodeIndex + newIndex].id
                        : newNode.childNodes[newNode.childNodes.length - 1].id;
                const subNodeParentNode = newNode;
                dispatch(fundSelectSubNode(newActiveFund.versionId, subNodeId, subNodeParentNode, false, null, true));
            }
        }
    };
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function _fundSubNodesNextPage(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_FUND_SUBNODES_NEXT_PAGE,
        versionId,
        nodeId,
        routingKey,
    };
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodesPrevPage(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        const r = findByRoutingKeyInGlobalState(getState(), versionId, routingKey);
        const node = r.node;
        let index = node.nodeIndex;
        dispatch(_fundSubNodesPrevPage(versionId, nodeId, routingKey));

        const state = getState();

        const nodes = state.arrRegion.funds[state.arrRegion.activeIndex].fundTree.nodes;
        const x = indexById(nodes, node.selectedSubNodeId);
        const xn = nodes[x];
        let subNodeIndex = x + 1;
        for (let i = x; i >= 0; i--) {
            const n = nodes[i];
            if (n.depth === xn.depth) {
                subNodeIndex--;
            } else {
                if (n.depth < xn.depth) {
                    break;
                }
            }
        }

        if (index != null) {
            const rnew = findByRoutingKeyInGlobalState(state, versionId, routingKey);
            const newActiveFund = rnew.fund;
            const newNode = rnew.node;
            const newIndex = index - node.pageSize;
            const subNodeId = newIndex < 0 ? nodes[subNodeIndex].id : nodes[subNodeIndex + newIndex].id;
            const subNodeParentNode = newNode;
            dispatch(fundSelectSubNode(newActiveFund.versionId, subNodeId, subNodeParentNode, false, null, true));
        }
    };
}

/**
 * Stránkování v Accordion - předchozí stránka.
 * {int} versionId verze AS
 * {int} nodeId id node dané záložky NODE
 * {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
function _fundSubNodesPrevPage(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_FUND_SUBNODES_PREV_PAGE,
        versionId,
        nodeId,
        routingKey,
    };
}
