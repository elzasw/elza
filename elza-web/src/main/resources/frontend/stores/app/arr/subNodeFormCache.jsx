import * as types from 'actions/constants/ActionTypes';
import {indexById, findByNodeKeyInNodes, selectedAfterClose} from 'stores/app/utils.jsx'
import {node, nodeInitState} from './node.jsx'
import {consolidateState} from 'components/Utils'

const subNodeFormCacheInitialState = {
    dataCache: {}
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
        case types.FA_SUB_NODE_FORM_CACHE_RESPONSE:
            var dataCache = {...state.dataCache}

            Object.keys(action.formsMap).forEach(nodeId => {
                dataCache[nodeId] = action.formsMap[nodeId]
            })

            return {
                ...state,
                dataCache: dataCache,
            }
        default:
            return state
    }
}
