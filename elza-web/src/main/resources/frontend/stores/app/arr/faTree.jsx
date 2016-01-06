import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components'

const initialState = {
    selectedId: null,
    focusId: null,
    expandedIds: {'0': true},
    searchedIds: null,
    isFetching: false,
    fetched: false,
    fetchingIncludeIds: {},   // jaké id aktuálně fetchuje - id na true
    nodes: [],
}
/*console.log('eeeeeeeeeeeeexxxxxxxxxxxxppp');
for (var a=0; a<300000; a++) {
initialState.expandedIds[a] = true;
}*/

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
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            return Object.assign({}, state, {focusId: null});
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
                        return Object.assign({}, state, {
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
                    nodes: action.nodes,
                    expandedIds: action.expandedIds,
                    fetchingIncludeIds: {},
                    lastUpdated: action.receivedAt
                })

                action.expandedIdsExtension.forEach(id => {
                    result.expandedIds[id] = true;
                });

                return result;
            }
        default:
            return state
    }
}
