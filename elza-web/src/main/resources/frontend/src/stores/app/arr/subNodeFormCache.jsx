import * as types from 'actions/constants/ActionTypes.js';
import {indexById, findByRoutingKeyInNodes, selectedAfterClose} from 'stores/app/utils.jsx'
import {consolidateState} from 'components/Utils.jsx'

const subNodeFormCacheInitialState = {
    dataCache: {},
    isFetching: false,
}

export default function subNodeFormCache(state = subNodeFormCacheInitialState, action) {
    switch (action.type) {
        case types.CHANGE_NODES:
            var dataCache = {...state.dataCache}
            var found = false
            action.nodeIds.forEach(nodeId => {
                if (dataCache[nodeId]) {    // má ji nakešovanou, odebereme ji, protože je neplatná
                    delete dataCache[nodeId]
                    found = true
                }
            })
            if (found) {
                return {
                    ...state,
                    dataCache: dataCache
                }
            } else {
                return state
            }
        case types.FUND_SUB_NODE_FORM_CACHE_REQUEST:
            return {
                ...state,
                isFetching: true,
            }
        case types.FUND_SUB_NODE_FORM_CACHE_RESPONSE:
            var dataCache = {...state.dataCache}

            Object.keys(action.formsMap).forEach(nodeId => {
                dataCache[nodeId] = action.formsMap[nodeId]
            })

            return {
                ...state,
                isFetching: false,
                dataCache: dataCache,
            }

        case types.NODES_DELETE:
        case types.FUND_INVALID:
            return subNodeFormCacheInitialState;

        default:
            return state
    }
}
