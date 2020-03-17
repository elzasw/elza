import * as types from 'actions/constants/ActionTypes.js';

const subNodeFormCacheInitialState = {
    dataCache: {},
    isFetching: false,
};

export default function subNodeFormCache(state = subNodeFormCacheInitialState, action) {
    let dataCache;
    switch (action.type) {
        case types.CHANGE_NODES:
            dataCache = {...state.dataCache};
            let found = false;
            action.nodeIds.forEach(nodeId => {
                if (dataCache[nodeId]) {
                    // má ji nakešovanou, odebereme ji, protože je neplatná
                    delete dataCache[nodeId];
                    found = true;
                }
            });
            if (found) {
                return {
                    ...state,
                    dataCache: dataCache,
                };
            } else {
                return state;
            }
        case types.FUND_SUB_NODE_FORM_CACHE_REQUEST:
            return {
                ...state,
                isFetching: true,
            };
        case types.FUND_SUB_NODE_FORM_CACHE_RESPONSE:
            dataCache = {...state.dataCache};

            Object.keys(action.formsMap).forEach(nodeId => {
                dataCache[nodeId] = action.formsMap[nodeId];
            });

            return {
                ...state,
                isFetching: false,
                dataCache: dataCache,
            };

        case types.NODES_DELETE:
        case types.FUND_INVALID:
            return subNodeFormCacheInitialState;

        default:
            return state;
    }
}
