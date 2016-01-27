import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components'

const initialState = {
    selectedId: null,
    selectedIds: {},
    focusId: null,
    expandedIds: {},
    searchedIds: [],
    filterText: null,
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
export default function faTree(state = initialState, action) {
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
        case types.STORE_SAVE:
            const {selectedId, selectedIds, focusId, expandedIds, searchedIds, filterText, filterCurrentIndex, multipleSelection, multipleSelectionOneLevel} = state;
            return {
                selectedId,
                selectedIds,
                focusId,
                expandedIds,
                searchedIds,
                filterText,
                filterCurrentIndex,
                multipleSelection,
                multipleSelectionOneLevel,
            }     
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            return Object.assign({}, state, {focusId: null});
        case types.FA_FA_TREE_FULLTEXT_CHANGE:
            return {...state, filterText: action.filterText}
        case types.FA_FA_TREE_FULLTEXT_RESULT:
            if (state.filterText == action.filterText) {    // jen pokud výsledek odpovídá aktuálnímu stavu v hledací komponentě
                return {...state, filterCurrentIndex: -1, searchedIds: action.searchedIds}
            } else {
                return state;
            }
        case types.FA_FA_TREE_SELECT_NODE:
            if (state.multipleSelection) {
                var newState = {...state, lastSelectedId: null}
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
                return Object.assign({}, state, {selectedId: action.nodeId});
            }
        case types.FA_FA_SELECT_SUBNODE:
            return Object.assign({}, state, {selectedId: action.subNodeId});
        case types.FA_FA_TREE_FOCUS_NODE:
            return Object.assign({}, state, {focusId: action.node.id});
        case types.FA_FA_TREE_EXPAND_NODE:
            if (action.addWaitingNode) {
                var index = indexById(state.nodes, action.node.id);
                return Object.assign({}, state, {
                    expandedIds: {...state.expandedIds, [action.node.id]: true},
                    nodes: [
                        ...state.nodes.slice(0, index + 1),
                        {id: '___' + Math.random(), name: i18n('global.data.loading'), depth: action.node.depth + 1},
                        ...state.nodes.slice(index + 1)
                    ],
                });
            } else {
                return Object.assign({}, state, {
                    expandedIds: {...state.expandedIds, [action.node.id]: true}
                });
            }
        case types.FA_FA_TREE_COLLAPSE_NODE:
            var expandedIds = {...state.expandedIds};
            delete expandedIds[action.node.id];

            var removeInfo = removeChildren(state.nodes, action.node, state.selectedId);

            var newSelectedId = state.selectedId;
            if (state.selectedId != null && removeInfo.containsSelectedId) {    // zabaloval se podtrom, který měl označnou položku
                // Položku odznačíme
                newSelectedId = null;
            }

            var ret = Object.assign({}, state, {
                expandedIds: expandedIds,
                nodes: removeInfo.nodes,
                selectedId: newSelectedId,
            });
            return ret;
        case types.FA_FA_TREE_REQUEST:
            var fetchingIncludeIds = [];
            if (action.includeIds != null) {
                action.includeIds.forEach(id => {
                    fetchingIncludeIds[id] = true;
                });
            }
            return Object.assign({}, state, {
                isFetching: true,
                fetchingIncludeIds: fetchingIncludeIds
            })
        case types.FA_FA_TREE_RECEIVE:
            if (action.nodeId !== null && typeof action.nodeId !== 'undefined') {
                if (state.expandedIds[action.nodeId]) { // ještě je stále rozbalený
                    var index = indexById(state.nodes, action.nodeId);
                    if (index != null) {
                        var node = state.nodes[index];
                        var removeInfo = removeChildren(state.nodes, node, null);
                        var nodes = removeInfo.nodes;
                        var result = Object.assign({}, state, {
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
                        return Object.assign({}, state, { fetchingIncludeIds: {} });
                    }
                } else {
                    return Object.assign({}, state, { fetchingIncludeIds: {} });
                }
            } else {
                var result = Object.assign({}, state, {
                    isFetching: false,
                    fetched: true,
                    dirty: false,
                    nodes: action.nodes,
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

        case types.CHANGE_CONFORMITY_INFO:
            var index = indexById(state.nodes, action.nodeId);

            // pouze, pokud ho mám načtený
            if (index != null) {
                return Object.assign({}, state, { dirty: true });
            }

            return state;

        default:
            return state
    }
}
