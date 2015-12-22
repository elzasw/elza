import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components'

const initialState = {
    faId: null,
    versionId: null,
    selectedId: null,
    focusId: null,
    expandedIds: {'n_0': true},
    searchedIds: null,
    isFetching: false,
    fetched: false,
    nodes: [],
}
/*console.log('eeeeeeeeeeeeexxxxxxxxxxxxppp');
for (var a=0; a<300000; a++) {
initialState.expandedIds['n_' + a] = true;
}*/

function removeChildren(nodes, node) {
    var index = indexById(nodes, node.id);
    var start = index;
    var max = nodes.length;
    while (++index < max) {
        if (nodes[index].depth > node.depth) { // potomek, odebereme
            // ale až na konci
        } else {    // už není potomek, končíme procházení
            break;
        }
    }

    return [
        ...nodes.slice(0, start + 1),
        ...nodes.slice(index)
    ]
}
export default function faTree(state = initialState, action) {
    switch (action.type) {
        case types.FA_FA_SELECT_SUBNODE:
            return Object.assign({}, state, {selectedId: action.subNodeId});
        case types.FA_FA_TREE_FOCUS_NODE:
            return Object.assign({}, state, {focusId: action.node.id});
        case types.FA_FA_TREE_EXPAND_NODE:
            if (action.addWaitingNode) {
                var index = indexById(state.nodes, action.node.id);
                return Object.assign({}, state, {
                    expandedIds: {...state.expandedIds, ['n_' + action.node.id]: true},
                    nodes: [
                        ...state.nodes.slice(0, index + 1),
                        {id: '___' + Math.random(), name: i18n('global.data.loading'), depth: action.node.depth + 1},
                        ...state.nodes.slice(index + 1)
                    ],
                });
            } else {
                return Object.assign({}, state, {
                    expandedIds: {...state.expandedIds, ['n_' + action.node.id]: true}
                });
            }
        case types.FA_FA_TREE_COLLAPSE_NODE:
            var expandedIds = {...state.expandedIds};
            delete expandedIds['n_' + action.node.id];
            var ret = Object.assign({}, state, {
                expandedIds: expandedIds,
                nodes: removeChildren(state.nodes, action.node),
            });
            return ret;
        case types.FA_FA_TREE_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
                faId: action.faId,
                versionId: action.versionId,
            })
        case types.FA_FA_TREE_RECEIVE:
            if (action.nodeId !== null && typeof action.nodeId !== 'undefined') {
                if (state.expandedIds['n_' + action.nodeId]) { // ještě je stále rozbalený
                    var index = indexById(state.nodes, action.nodeId);
                    if (index != null) {
                        var node = state.nodes[index];
                        var nodes = removeChildren(state.nodes, node);
                        return Object.assign({}, state, {
                            isFetching: false,
                            fetched: true,
                            nodes: [
                                ...nodes.slice(0, index + 1),
                                ...action.nodes,
                                ...nodes.slice(index + 1)
                            ],
                            faId: action.faId,
                            versionId: action.versionId,
                            lastUpdated: action.receivedAt
                        })
                    } else {
                        return state;
                    }
                } else {
                    return state;
                }
            } else {
                return Object.assign({}, state, {
                    isFetching: false,
                    fetched: true,
                    nodes: action.nodes,
                    expandedIds: action.expandedIds,
                    faId: action.faId,
                    versionId: action.versionId,
                    lastUpdated: action.receivedAt
                })
            }
        default:
            return state
    }
}
