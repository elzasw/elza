/**
 * Akce pro strom AS.
 * Vysvětlení pojmů:
 * uzel - JP
 */

import {WebApi} from 'actions/index';
import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils';
import {fundSelectSubNode} from './node';
import {createFundRoot} from 'components/arr/ArrUtils';

export function isFundTreeAction(action) {
    switch (action.type) {
        case types.FUND_FUND_TREE_FULLTEXT_CHANGE:
        case types.FUND_FUND_TREE_CONFIGURE:
        case types.FUND_FUND_TREE_FOCUS_NODE:
        case types.FUND_FUND_TREE_EXPAND_NODE:
        case types.FUND_FUND_TREE_COLLAPSE:
        case types.FUND_FUND_TREE_COLLAPSE_NODE:
        case types.FUND_FUND_TREE_SELECT_NODE:
        case types.FUND_FUND_TREE_REQUEST:
        case types.FUND_FUND_TREE_RECEIVE:
        case types.FUND_FUND_TREE_FULLTEXT_RESULT:
        case types.SELECT_FUND_GLOBAL:
        case types.FUND_FUND_TREE_INVALIDATE:
            return true;
        default:
            return false;
    }
}

// jen vyber polozky, vyuzite jen v presunech JP
export function fundTreeSelectNode(
    area,
    versionId,
    nodeId,
    ctrl,
    shift,
    newFilterCurrentIndex = null,
    ensureItemVisible = false,
) {
    return {
        type: types.FUND_FUND_TREE_SELECT_NODE,
        area,
        versionId,
        nodeId,
        ctrl,
        shift,
        newFilterCurrentIndex,
        ensureItemVisible,
    };
}

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {Object} node uzel
 * @param {boolean} pokud je true, je pod rozbalovaný uzel přidán nový uzel s informací, že se načítají data
 */
export function _fundTreeNodeExpand(area, versionId, node, addWaitingNode = false) {
    return {
        type: types.FUND_FUND_TREE_EXPAND_NODE,
        area,
        node,
        versionId,
        addWaitingNode,
    };
}

/**
 * Konfigurace stromu - např. multiple selection atp.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {boolean} multipleSelection má podporovat multiple selection?
 * @param {boolean} multipleSelectionOneLevel má podporovat multiple selection jen na jedné úrovni?
 */
export function fundTreeConfigure(area, versionId, multipleSelection, multipleSelectionOneLevel) {
    return {
        type: types.FUND_FUND_TREE_CONFIGURE,
        area,
        versionId,
        multipleSelection,
        multipleSelectionOneLevel,
    };
}

/**
 * Zabalení uzlu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {Object} node uzel
 */
export function fundTreeCollapse(area, versionId, node) {
    return {
        type: types.FUND_FUND_TREE_COLLAPSE,
        area,
        node,
        versionId,
    };
}

/**
 * Hledání ve stromu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {string} filterText hledaný text
 */
export function fundTreeFulltextChange(area, versionId, filterText) {
    return {
        type: types.FUND_FUND_TREE_FULLTEXT_CHANGE,
        area,
        versionId,
        filterText,
    };
}

/**
 * Dohledání konkrétního stromu.
 * {Object} state root store
 * {string} area jaký strom
 * {int} versionId verze AS
 */
function getFundTreeForFund(state, area, versionId) {
    var fundTree;
    if (area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        // fundRegion
        return state.fundRegion.fundDetail.fundTree;
    } else if (area === types.FUND_TREE_AREA_COPY) {
        return state.arrRegion.globalFundTree.fundTreeCopy;
    } else if (area === types.CUSTOM_FUND_TREE_AREA_NODES) {
        return state.arrRegion.customFund.fundTreeNodes;
    } else {
        // arrRegion
        var index = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (index != null) {
            var fund = state.arrRegion.funds[index];
            var fundTree = getFundTree(fund, area);

            return fundTree;
        }
    }

    return null;
}

function getFundForTree(state, area, versionId) {
    var fundTree;
    if (area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        // fundRegion
        return state.fundRegion;
    } else if (area === types.FUND_TREE_AREA_COPY) {
        return state.arrRegion.globalFundTree.fundTreeCopy;
    } else if (area === types.CUSTOM_FUND_TREE_AREA_NODES) {
        return state.arrRegion.customFund.fundTreeNodes;
    } else {
        // arrRegion
        var index = indexById(state.arrRegion.funds, versionId, 'versionId');
        if (index != null) {
            return state.arrRegion.funds[index];
        }
    }

    return null;
}

/**
 * Změna aktuálně vybrané položky z možných výsledků hledání.
 * {func} dispatch dispatch
 * {string} area jaký strom
 * {object} fund fond
 * {Object} fundTree store stromu
 * {int} newIndex nový vybraný index ve výsledcích hledání
 */
function changeCurrentIndex(dispatch, area, fund, versionId, fundTree, newIndex) {
    if (newIndex != fundTree.filterCurrentIndex) {
        const nodeId = fundTree.searchedIds[newIndex];
        let nodeParent = fundTree.searchedParents[nodeId];

        if (nodeParent === null) {
            nodeParent = createFundRoot(fund);
        }
        switch (area) {
            case types.FUND_TREE_AREA_MAIN:
                dispatch(fundSelectSubNode(versionId, nodeId, nodeParent, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_COPY:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, nodeParent, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_USAGE:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, nodeParent, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_MOVEMENTS_LEFT:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_MOVEMENTS_RIGHT:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_FUNDS_FUND_DETAIL:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_NODES:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_DAOS_LEFT:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            case types.FUND_TREE_AREA_DAOS_RIGHT:
                dispatch(fundTreeSelectNode(area, versionId, nodeId, false, false, newIndex, true));
                break;
            default:
                break;
        }
    }
}

/**
 * Přesun na další položku vy výsledcích hledání.
 * {string} area jaký strom
 * {int} versionId verze AS
 */
export function fundTreeFulltextNextItem(area, versionId) {
    return (dispatch, getState) => {
        const state = getState();
        const fundTree = getFundTreeForFund(state, area, versionId);
        if (fundTree && fundTree.searchedIds.length > 0) {
            let newIndex;
            if (fundTree.filterCurrentIndex == -1) {
                newIndex = 0;
            } else {
                newIndex = Math.min(fundTree.filterCurrentIndex + 1, fundTree.searchedIds.length - 1);
            }
            changeCurrentIndex(dispatch, area, getFundForTree(state, area, versionId), versionId, fundTree, newIndex);
        }
    };
}

/**
 * Přesun na předchozí položku vy výsledcích hledání.
 * {string} area jaký strom
 * {int} versionId verze AS
 */
export function fundTreeFulltextPrevItem(area, versionId) {
    return (dispatch, getState) => {
        const state = getState();
        const fundTree = getFundTreeForFund(state, area, versionId);

        if (fundTree && fundTree.searchedIds.length > 0) {
            let newIndex;
            if (fundTree.filterCurrentIndex == -1) {
                newIndex = 0;
            } else {
                newIndex = Math.max(fundTree.filterCurrentIndex - 1, 0);
            }
            changeCurrentIndex(dispatch, area, getFundForTree(state, area, versionId), versionId, fundTree, newIndex);
        }
    };
}

/**
 * Akce fulltextového hledání ve stromu. Text, podle kterého se hledá, je brán ze store konkrétního stromu.
 * {string} area jaký strom
 * {int} versionId verze AS
 */
export function fundTreeFulltextSearch(area, versionId, params, result, luceneQuery) {
    return (dispatch, getState) => {
        var state = getState();
        var fundTree = getFundTreeForFund(state, area, versionId);
        if (fundTree) {
            if ((fundTree.filterText && fundTree.filterText.length > 0) || luceneQuery) {
                var searchParentNodeId;
                // V případě stromu, který má multi select hledáme v celém stromu
                // V případě stromu, který má single select hledáme POD vybranou položkou
                if (fundTree.multipleSelection) {
                    // hledáme v celém stromu
                    searchParentNodeId = null;
                } else {
                    // hledáme pod vybranou položkou, pokud nějaká je
                    if (fundTree.selectedId !== null) {
                        // vybraná položka, hledáme pod ní
                        searchParentNodeId = fundTree.selectedId;
                    } else {
                        // nice není vybráno, hledáme v celém stromě
                        searchParentNodeId = null;
                    }
                }

                return WebApi.findInFundTree(
                    versionId,
                    searchParentNodeId,
                    fundTree.filterText,
                    'SUBTREE',
                    params,
                    luceneQuery,
                ).then(json => {
                    dispatch(
                        fundTreeFulltextResult(area, versionId, fundTree.filterText, json, false, result, luceneQuery),
                    );
                    if (json.length > 0) {
                        var newFundTree = getFundTreeForFund(getState(), area, versionId);
                        changeCurrentIndex(
                            dispatch,
                            area,
                            getFundForTree(state, area, versionId),
                            versionId,
                            newFundTree,
                            0,
                        );
                    }
                });
            } else {
                return dispatch(fundTreeFulltextResult(area, versionId, fundTree.filterText, [], true, null, false));
            }
        }
    };
}

/**
 * Výsledek hledání ve stromu.
 * {string} area jaký strom
 * {int} versionId verze AS
 * {string} filterText pro jaký hledaný text platí výsledky
 * {Arraz} searchedData seznam nalezených node
 * {boolean} clearFilter jedná se o akci, která má pouze vymazat aktuální filtr?
 */
export function fundTreeFulltextResult(
    area,
    versionId,
    filterText,
    searchedData,
    clearFilter,
    searchFormData,
    luceneQuery,
) {
    return {
        type: types.FUND_FUND_TREE_FULLTEXT_RESULT,
        area,
        versionId,
        filterText,
        searchedData,
        clearFilter,
        searchFormData,
        luceneQuery,
    };
}

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function fundTreeNodeExpand(area, node) {
    return (dispatch, getState) => {
        var state = getState();
        var fundTree;
        var versionId;
        let activeFund, activeNode;
        if (area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
            // fundRegion
            versionId = state.fundRegion.fundDetail.versionId;
            fundTree = state.fundRegion.fundDetail.fundTree;
        } else if (area === types.CUSTOM_FUND_TREE_AREA_NODES) {
            versionId = state.arrRegion.customFund.versionId;
            fundTree = state.arrRegion.customFund.fundTreeNodes;
        } else if (area === types.FUND_TREE_AREA_COPY) {
            versionId = state.arrRegion.globalFundTree.versionId;
            fundTree = state.arrRegion.globalFundTree.fundTreeCopy;
        } else {
            // arrRegion
            activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
            versionId = activeFund.versionId;
            fundTree = getFundTree(activeFund, area);
        }
        dispatch(_fundTreeNodeExpand(area, versionId, node, true));

        var nodeId = node.id;
        var expandedIds = {...fundTree.expandedIds, [nodeId]: true};
        if (activeNode && activeNode.selectedSubNodeId === nodeId) {
            /*let json = {
                nodes:[...activeNode.subNodeInfo.childNodes],
                expandedIdsExtension:[]
            };
            console.log("skip fundTree", activeNode, json)*/
            //return dispatch(fundTreeReceive(area, versionId, nodeId, expandedIds, [], json));
        }
        return WebApi.getFundTree(versionId, nodeId, expandedIds).then(json =>
            dispatch(fundTreeReceive(area, versionId, nodeId, expandedIds, [], json)),
        );
    };
}

/**
 * Nastavení focusu pro uzel ve stromu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {Object} node uzel
 */
export function fundTreeFocusNode(area, versionId, node) {
    return {
        type: types.FUND_FUND_TREE_FOCUS_NODE,
        area,
        node,
        versionId,
    };
}

/**
 * Zabalení uzlu.
 * @param {String} area oblast stromu
 * @param {int} versionId verze AS
 * @param {Object} node uzel
 */
export function fundTreeNodeCollapse(area, versionId, node) {
    return {
        type: types.FUND_FUND_TREE_COLLAPSE_NODE,
        area,
        node,
        versionId,
    };
}

/**
 * Získání store konkrétního store stromu na daný AS a oblast stromu.
 * {Object} fund store fund
 * {string} area o jaký strom se jedná
 */
function getFundTree(fund, area) {
    switch (area) {
        case types.FUND_TREE_AREA_MAIN:
            return fund.fundTree;
        case types.FUND_TREE_AREA_MOVEMENTS_LEFT:
            return fund.fundTreeMovementsLeft;
        case types.FUND_TREE_AREA_MOVEMENTS_RIGHT:
            return fund.fundTreeMovementsRight;
        case types.FUND_TREE_AREA_NODES:
            return fund.fundTreeNodes;
        case types.FUND_TREE_AREA_DAOS_LEFT:
            return fund.fundTreeDaosLeft;
        case types.FUND_TREE_AREA_DAOS_RIGHT:
            return fund.fundTreeDaosRight;
        default:
            break;
    }
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {String} area oblast stromu
 * @param {sourceVersionId} id verze
 * @param {expandedIds} seznam rozbalených uzlů
 * @param selectedIdInfo buď id nebo pole id (id, které je aktuálně vybrané nebo null, toto id bude také načtené a strom zobrazený s touto položkou)
 */
export function fundTreeFetchIfNeeded(area, sourceVersionId, expandedIds, selectedIdInfo) {
    return (dispatch, getState) => {
        const state = getState();

        let fundTree;
        let versionId;
        if (area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
            // fundRegion
            versionId = state.fundRegion.fundDetail.versionId;
            fundTree = state.fundRegion.fundDetail.fundTree;
        } else if (area === types.FUND_TREE_AREA_COPY) {
            versionId = state.arrRegion.globalFundTree.versionId;
            fundTree = state.arrRegion.globalFundTree.fundTreeCopy;
        } else if (area === types.CUSTOM_FUND_TREE_AREA_NODES) {
            versionId = state.arrRegion.customFund.versionId;
            fundTree = state.arrRegion.customFund.fundTreeNodes;
        } else {
            // arrRegion
            const activeFund = state.arrRegion.funds[state.arrRegion.activeIndex];
            versionId = activeFund.versionId;
            fundTree = getFundTree(activeFund, area);
        }

        let fetch = false;
        let includeIds = [];
        let selectedIds;

        if (fundTree.multipleSelection) {
            selectedIds = Object.keys(fundTree.selectedIds);
        } else {
            if (typeof fundTree.selectedId !== 'undefined' && fundTree.selectedId !== null) {
                selectedIds = [fundTree.selectedId];
            } else {
                selectedIds = [];
            }
        }
        if (selectedIds.length > 0) {
            selectedIds.forEach(selectedId => {
                const id = parseInt(selectedId);
                includeIds.push(id);

                const isInView = indexById(fundTree.nodes, id);
                if (isInView == null) {
                    if (!fundTree.fetchingIncludeIds[id]) {
                        fetch = true;
                    }
                }
            });
        }

        if (!fundTree.fetched && !fundTree.isFetching) {
            fetch = true;
        }

        if (fundTree.dirty && !fundTree.isFetching) {
            fetch = true;
        }

        if (fetch) {
            dispatch(fundTreeFetch(area, versionId, null, expandedIds, includeIds));
        }

        return Promise.resolve(fundTree);
    };
}

/**
 * Nové načtení dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} pokud je uvedeno, obsahuje id node, pro který se mají vracet data, pokud není uveden, vrací se celý strom
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function fundTreeFetch(area, versionId, nodeId, expandedIds, includeIds = []) {
    return dispatch => {
        dispatch(fundTreeRequest(area, versionId, nodeId, expandedIds, includeIds));
        return WebApi.getFundTree(versionId, nodeId, expandedIds, includeIds).then(json => {
            dispatch(fundTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json));
            return json;
        });
    };
}

/**
 * Nová data byla načtena.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 * @param {Object} json objekt s daty
 */
export function fundTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json) {
    return {
        type: types.FUND_FUND_TREE_RECEIVE,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
        nodes: json.nodes,
        expandedIdsExtension: json.expandedIdsExtension,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function fundTreeRequest(area, versionId, nodeId, expandedIds, includeIds) {
    return {
        type: types.FUND_FUND_TREE_REQUEST,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
    };
}
