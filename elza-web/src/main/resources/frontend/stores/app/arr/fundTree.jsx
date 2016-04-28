import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/index.jsx';
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

function removeChildren(nodes, node, selectedId) {
    var index = indexById(nodes, node.id);
    var start = index;
    var max = nodes.length;
    var containsSelectedId = false;
    while (++index < max) {
        if (nodes[index].depth > node.depth) { // potomek, odebereme
            // ale až na konci
            if (selectedId != null && selectedId == nodes[index].id) {
                containsSelectedId = true;
            }
        } else {    // už není potomek, končíme procházení
            break;
        }
    }

    return {
        containsSelectedId: containsSelectedId,
        nodes: [
            ...nodes.slice(0, start + 1),
            ...nodes.slice(index)
        ]
    }
}
export default function fundTree(state = initialState, action) {
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

        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_DELETE_LEVEL:

            var refresh = false;

            if (action.nodeId == state.selectedId || action.parentNodeId == state.selectedId) {
                refresh = true;
            }

            state.nodes.forEach(node => {
                if (node.id == action.nodeId || node.id == action.parentNodeId) {
                refresh = true;
                }
            });

            if (refresh) {
                return {...state, selectedId: null}
            }

            return state;

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
                searchedParents: []
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
                    searchedParents: searchedParents
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
                        {id: '___' + Math.random(), name: i18n('global.data.loading'), depth: action.node.depth + 1},
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

            var removeInfo = removeChildren(state.nodes, action.node, state.selectedId);

            var newSelectedId = state.selectedId;
            if (state.selectedId != null && removeInfo.containsSelectedId) {    // zabaloval se podtrom, který měl označnou položku
                // Položku odznačíme
                newSelectedId = null;
            }

            var ret = Object.assign({}, state, {
                ensureItemVisible: false,
                expandedIds: expandedIds,
                nodes: removeInfo.nodes,
                selectedId: newSelectedId,
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
                        var removeInfo = removeChildren(state.nodes, node, null);
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
            var isDirty = false;
            var nodeId;
            for(var i = 0; i < state.nodes.length; i++) {
                nodeId = state.nodes[i].id;
                if (action.nodeIds.indexOf(nodeId) >= 0) {
                    isDirty = true;
                    break;
                }
            }
            // pouze, pokud mám některý načtený
            if (isDirty) {
                return Object.assign({}, state, { ensureItemVisible: false, dirty: true });
            }

            return state;

        default:
            return state
    }
}
