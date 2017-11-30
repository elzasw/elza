import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/shared';
import {consolidateState} from 'components/Utils.jsx'

const initialState = {
    selectedId: null,
    selectedIds: {},
    focusId: null,
    expandedIds: {},
    searchedIds: [],
    ensureItemVisible: false,
    searchedParents: {},
    filterText: null,
    filterResult: false,
    filterCurrentIndex: -1,
    isFetching: false,
    fetched: false,
    dirty: false,
    fetchingIncludeIds: {},   // jaké id aktuálně fetchuje - id na true
    nodes: [],
    lastSelectedId: null,
    multipleSelection: false,
    multipleSelectionOneLevel: false,
    luceneQuery: false,
}

function getOneSelectedIdIfExists(state) {
    if (state.multipleSelection && Object.keys(state.selectedIds).length == 1) {
        return Object.keys(state.selectedIds)[0];
    } else if (!state.multipleSelection && state.selectedId != null) {
        return state.selectedId;
    } else {
        return null;
    }
}

function removeChildren(nodes, collapsedNode, selectedIdsMap) {
    var index = indexById(nodes, collapsedNode.id);
    var start = index;
    var max = nodes.length;
    var containsSelectedIds = [];

    while (++index < max) {
        let node = nodes[index]
        if (node.depth > collapsedNode.depth) { // potomek, odebereme
            // ale až na konci
            if (selectedIdsMap[node.id]) {
                containsSelectedIds.push(node.id)
            }
        } else {    // už není potomek, končíme procházení
            break;
        }
    }

    return {
        containsSelectedIds: containsSelectedIds,
        nodes: [
            ...nodes.slice(0, start + 1),
            ...nodes.slice(index)
        ]
    }
}
export default function fundTree(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                fetched: false,
                dirty: false,
                fetchingIncludeIds: {},
                nodes: [],
            }

        case types.FUND_FUND_TREE_CONFIGURE: {
            let selectedIds = state.selectedIds;
            let selectedId = state.selectedId;

            if (state.multipleSelection && !action.multipleSelection) { // pokud je označeno více položek, musíme je odznačit
                selectedId = null;
                selectedIds = {};
            } else if (!state.multipleSelection && action.multipleSelection) {
                if (state.selectedId !== null) {
                    selectedIds = { [selectedId]: true };
                    selectedId = null;
                }
            } else if (state.multipleSelection && action.multipleSelection) {
                if (Object.keys(selectedIds).length > 1) {
                    if (!state.multipleSelectionOneLevel && action.multipleSelectionOneLevel) { // pokud aktuální označení nevyhovuje, zrušíme ho
                        const keys = Object.keys(state.selectedIds);
                        let sameLevel = true;
                        const testLevel = state.nodes[indexById(state.nodes, keys[0])].depth;
                        for (let a=1; a<keys.length; a++) {
                            const currLevel = state.nodes[indexById(state.nodes, keys[a])].depth;
                            if (currLevel !== testLevel) {
                                sameLevel = false;
                                break;
                            }
                        }
                        if (!sameLevel) {
                            selectedIds = {};
                        }
                    }
                }
            }

            return {
                ...state,
                selectedIds,
                selectedId,
                multipleSelection: action.multipleSelection,
                multipleSelectionOneLevel: action.multipleSelectionOneLevel,
            }
        }
        case types.CHANGE_ADD_LEVEL:
            return {...state, dirty: true}

        case types.CHANGE_DELETE_LEVEL: {

            var refresh = false;

            if (action.nodeId == state.selectedId || action.parentNodeId == state.selectedId) {
                refresh = true;
            }

            for (let i = 0; i < state.nodes.length; i++) {
                const node = state.nodes[i];
                if (node.id == action.nodeId || node.id == action.parentNodeId) {
                    refresh = true;
                    break;
                }
            }

            if (refresh) {
                return {...state, dirty: true, selectedId: null}
            }

            return state;
        }

        case types.FUND_NODE_CHANGE:
            var index = indexById(state.nodes, action.parentNode.id);
            if (index != null) {
                state.nodes[index].version = action.parentNode.version;
            }
            return {...state, dirty: true}
        case types.STORE_SAVE:
            const {selectedId, selectedIds, focusId, expandedIds, searchedIds, filterText, filterCurrentIndex, multipleSelection, multipleSelectionOneLevel} = state;
            return {
                selectedId,
                selectedIds,
                focusId,
                expandedIds,
                multipleSelection,
                multipleSelectionOneLevel,
            }
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            var result = {
                ...state,
                focusId: null
            }
            return consolidateState(state, result);
        case types.FUND_FUND_TREE_FULLTEXT_CHANGE:
            return {
                ...state,
                filterResult: false,
                filterText: action.filterText,
                filterCurrentIndex: -1,
                searchedIds: [],
                searchedParents: [],
                luceneQuery: false
            }
        case types.FUND_FUND_TREE_FULLTEXT_RESULT:
            if (state.filterText == action.filterText) {    // jen pokud výsledek odpovídá aktuálnímu stavu v hledací komponentě
                var searchedIds = [];
                var searchedParents = {};
                action.searchedData.forEach(i => {
                    searchedIds.push(i.nodeId);
                    searchedParents[i.nodeId] = i.parent;
                })

                return {
                    ...state,
                    filterResult: !action.clearFilter,
                    filterCurrentIndex: -1,
                    ensureItemVisible: false,
                    searchedIds: searchedIds,
                    searchedParents: searchedParents,
                    searchFormData: action.searchFormData,
                    luceneQuery: action.luceneQuery
                }
            } else {
                return state;
            }
        case types.FUND_FUND_TREE_SELECT_NODE:
            if (state.multipleSelection) {
                var newCurrentIndex = state.filterCurrentIndex;
                if (action.newFilterCurrentIndex != null) {
                    newCurrentIndex = action.newFilterCurrentIndex;
                }

                var newState = {
                    ...state,
                    lastSelectedId: null,
                    ensureItemVisible: action.ensureItemVisible,
                    filterCurrentIndex: newCurrentIndex
                }
                newState.selectedIds = {...state.selectedIds};

                if (action.ctrl) {
                    if (newState.selectedIds[action.nodeId]) { // byl vybrán a je držen ctrl, odznačíme ho
                        delete newState.selectedIds[action.nodeId];
                    } else {    // přidáme na výběr
                        if (state.multipleSelectionOneLevel) {
                            var keys = Object.keys(newState.selectedIds);
                            if (keys.length > 0) {  // kontrolujeme stejný level - depth
                                var indexSelected = indexById(newState.nodes, keys[0]);
                                var indexNewSelection = indexById(newState.nodes, action.nodeId);
                                if (newState.nodes[indexSelected].depth == newState.nodes[indexNewSelection].depth) {
                                    newState.selectedIds[action.nodeId] = true;
                                    newState.lastSelectedId = action.nodeId;
                                }
                            } else {
                                newState.selectedIds[action.nodeId] = true;
                                newState.lastSelectedId = action.nodeId;
                            }
                        } else {
                            newState.selectedIds[action.nodeId] = true;
                            newState.lastSelectedId = action.nodeId;
                        }
                    }
                } else if (action.shift) {
                    if (state.lastSelectedId != null) {
                        var indexSelected = indexById(newState.nodes, state.lastSelectedId);
                        var indexNewSelection = indexById(newState.nodes, action.nodeId);

                        if (state.multipleSelectionOneLevel) {
                            var depth = newState.nodes[indexSelected].depth;
                            if (depth == newState.nodes[indexNewSelection].depth) {
                                for (var a=Math.min(indexSelected, indexNewSelection); a<= Math.max(indexSelected, indexNewSelection); a++) {
                                    if (state.nodes[a].depth == depth) {
                                        newState.selectedIds[state.nodes[a].id] = true;
                                    }
                                }
                            } else {
                                newState.lastSelectedId = state.lastSelectedId;
                            }
                        } else {
                            for (var a=Math.min(indexSelected, indexNewSelection); a<= Math.max(indexSelected, indexNewSelection); a++) {
                                newState.selectedIds[state.nodes[a].id] = true;
                            }
                        }
                    } else {
                        newState.selectedIds = {};
                        newState.selectedIds[action.nodeId] = true;
                        newState.lastSelectedId = action.nodeId;
                    }
                } else {    // odznačíme vše a vybereme předaný
                    newState.selectedIds = {};
                    newState.selectedIds[action.nodeId] = true;
                    newState.lastSelectedId = action.nodeId;
                }
                return newState;
            } else {
                if (state.selectedId !== action.nodeId || (action.newFilterCurrentIndex != null && state.filterCurrentIndex != action.newFilterCurrentIndex)) {
                    var newCurrentIndex = state.filterCurrentIndex;
                    if (action.newFilterCurrentIndex != null) {
                        newCurrentIndex = action.newFilterCurrentIndex;
                    }

                    return {
                        ...state,
                        ensureItemVisible: action.ensureItemVisible,
                        selectedId: action.nodeId,
                        filterCurrentIndex: newCurrentIndex
                    }
                } else {
                    return state;
                }
            }
        case types.FUND_FUND_SELECT_SUBNODE:
            if (state.selectedId !== action.subNodeId || (action.newFilterCurrentIndex != null && state.filterCurrentIndex != action.newFilterCurrentIndex)) {
                var newCurrentIndex = state.filterCurrentIndex;
                if (action.newFilterCurrentIndex != null) {
                    newCurrentIndex = action.newFilterCurrentIndex;
                }

                return {
                    ...state,
                    ensureItemVisible: action.ensureItemVisible,
                    selectedId: action.subNodeId,
                    filterCurrentIndex: newCurrentIndex
                }
            } else {
                return state;
            }
        case types.FUND_FUND_TREE_FOCUS_NODE:
            if (state.focusId !== action.node.id) {
                return {
                    ...state,
                    focusId: action.node.id,
                    ensureItemVisible: false
                };
            } else {
                return state;
            }
        case types.FUND_FUND_TREE_EXPAND_NODE:
            if (action.addWaitingNode) {
                var index = indexById(state.nodes, action.node.id);
                return Object.assign({}, state, {
                    expandedIds: {...state.expandedIds, [action.node.id]: true},
                    ensureItemVisible: false,
                    nodes: [
                        ...state.nodes.slice(0, index + 1),
                        {id: '___' + Math.random(), name: i18n('global.data.loading'), depth: action.node.depth + 1, isFetching: true},
                        ...state.nodes.slice(index + 1)
                    ],
                });
            } else {
                return Object.assign({}, state, {
                    ensureItemVisible: false,
                    expandedIds: {...state.expandedIds, [action.node.id]: true}
                });
            }
        case types.FUND_FUND_TREE_COLLAPSE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                ensureItemVisible: false,
                expandedIds: initialState.expandedIds,
                nodes: [
                    state.nodes[0]
                ],
                selectedId: initialState.selectedId,
                selectedIds: initialState.selectedIds
            });
        case types.FUND_FUND_TREE_COLLAPSE_NODE:
            var expandedIds = {...state.expandedIds};
            delete expandedIds[action.node.id];

            var selectedIdsMap
            if (state.multipleSelection) {
                selectedIdsMap = state.selectedIds
            } else {
                selectedIdsMap = {}
                if (state.selectedId !== null) {
                    selectedIdsMap[state.selectedId] = true
                }
            }
            var removeInfo = removeChildren(state.nodes, action.node, selectedIdsMap);

            let newSelectedId
            let newSelectedIds
            if (state.multipleSelection) {
                if (removeInfo.containsSelectedIds.length > 0) { // některá id jsou v seznamu zabalených
                    newSelectedIds = {...state.selectedIds}
                    removeInfo.containsSelectedIds.forEach(id => {
                        delete newSelectedIds[id]
                    })
                } else {
                    newSelectedIds = state.selectedIds
                }
            } else {
                if (removeInfo.containsSelectedIds.length > 0) { // dané id je v seznamu zabalených
                    newSelectedId = null
                } else {
                    newSelectedId = state.selectedId
                }
            }

            var ret = Object.assign({}, state, {
                ensureItemVisible: false,
                expandedIds: expandedIds,
                nodes: removeInfo.nodes,
                selectedId: newSelectedId,
                selectedIds: newSelectedIds,
            });
            return ret;
        case types.FUND_FUND_TREE_REQUEST:
            var fetchingIncludeIds = [];
            if (action.includeIds != null) {
                action.includeIds.forEach(id => {
                    fetchingIncludeIds[id] = true;
                });
            }
            return Object.assign({}, state, {
                isFetching: true,
                ensureItemVisible: false,
                fetchingIncludeIds: fetchingIncludeIds
            })
        case types.FUND_FUND_TREE_RECEIVE:
            if (action.nodeId !== null && typeof action.nodeId !== 'undefined') {
                if (state.expandedIds[action.nodeId]) { // ještě je stále rozbalený
                    var index = indexById(state.nodes, action.nodeId);
                    if (index != null) {
                        var node = state.nodes[index];
                        var removeInfo = removeChildren(state.nodes, node, {});
                        var nodes = removeInfo.nodes;

                        var ensureItemVisible = false;
                        var oneSelectedId = getOneSelectedIdIfExists(state);
                        if (oneSelectedId !== null) {
                            if (indexById(action.nodes, oneSelectedId) !== null) {    // je označená položka z těch, co se právě načetly
                                ensureItemVisible = true;
                            }
                        }

                        var result = Object.assign({}, state, {
                            ensureItemVisible: ensureItemVisible,
                            isFetching: false,
                            fetched: true,
                            nodes: [
                                ...nodes.slice(0, index + 1),
                                ...action.nodes,
                                ...nodes.slice(index + 1)
                            ],
                            fetchingIncludeIds: {},
                            lastUpdated: action.receivedAt
                        })

                        result.expandedIds = {...result.expandedIds};
                        action.expandedIdsExtension.forEach(id => {
                            result.expandedIds[id] = true;
                        });

                        return result;
                    } else {
                        return Object.assign({}, state, { ensureItemVisible: false, fetchingIncludeIds: {} });
                    }
                } else {
                    return Object.assign({}, state, { ensureItemVisible: false, fetchingIncludeIds: {} });
                }
            } else {
                var ensureItemVisible = false;
                var oneSelectedId = getOneSelectedIdIfExists(state);
                if (oneSelectedId !== null) {
                    if (indexById(action.nodes, oneSelectedId) !== null) {    // je označená položka z těch, co se právě načetly
                        ensureItemVisible = true;
                    }
                }

                var result = Object.assign({}, state, {
                    isFetching: false,
                    fetched: true,
                    dirty: false,
                    nodes: action.nodes,
                    ensureItemVisible: ensureItemVisible,
                    expandedIds: action.expandedIds,
                    fetchingIncludeIds: {},
                    lastUpdated: action.receivedAt
                })

                result.expandedIds = {...result.expandedIds};
                action.expandedIdsExtension.forEach(id => {
                    result.expandedIds[id] = true;
                });
                return result;
            }

        case types.CHANGE_MOVE_LEVEL:
            return Object.assign({}, state, { ensureItemVisible: false, dirty: true });

        case types.CHANGE_NODES:
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS:
            var isDirty = false;
            var nodeId;
            for(var i = 0; i < state.nodes.length; i++) {
                nodeId = state.nodes[i].id;
                if (action.nodeIds && action.nodeIds.indexOf(nodeId) >= 0) {
                    isDirty = true;
                    break;
                }
            }
            // pouze, pokud mám některý načtený
            if (isDirty) {
                return Object.assign({}, state, { ensureItemVisible: false, dirty: true });
            }

            return state;
        case types.FUND_SUBNODE_UPDATE:
            let data = action.data;
            let nodes = state.nodes;
            let nodeId = action.data.node ? action.data.node.id : action.data.parent.id;
            let index = indexById(nodes, nodeId);
            let updatedNode = nodes[index];

            for(let i in updatedNode){
                if(typeof data[i] !== "undefined"){
                    updatedNode[i] = data[i];
                }
            }
            state.nodes[index] = updatedNode;
            return {...state};
        case types.NODES_DELETE: {
            var result = {
                ...state
            };

            let expandedIds = {};
            Object.keys(result.expandedIds).forEach(function(key,index) {
                let nodeId = parseInt(key);
                if (action.nodeIds.indexOf(nodeId) < 0) {
                    expandedIds[nodeId] = true;
                }
            });
            result.expandedIds = expandedIds;

            let fetchingIncludeIds = {};
            Object.keys(result.fetchingIncludeIds).forEach(function(key,index) {
                let nodeId = parseInt(key);
                if (action.nodeIds.indexOf(nodeId) < 0) {
                    fetchingIncludeIds[nodeId] = true;
                }
            });
            result.fetchingIncludeIds = fetchingIncludeIds;

            let selectedIds = {};
            Object.keys(result.selectedIds).forEach(function(key,index) {
                let nodeId = parseInt(key);
                if (action.nodeIds.indexOf(nodeId) < 0) {
                    fetchingIncludeIds[nodeId] = true;
                }
            });
            result.selectedIds = selectedIds;

            if (result.selectedId !== null && action.nodeIds.indexOf(result.selectedId) >= 0) {
                result.selectedId = null;
            }

            return consolidateState(state, result);
        }

        default:
            return state
    }
}
