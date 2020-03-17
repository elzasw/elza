import * as types from 'actions/constants/ActionTypes.js';

export function globalFundTreeInvalidate() {
    return {
        area: types.FUND_TREE_AREA_COPY,
        type: types.FUND_FUND_TREE_INVALIDATE,
    };
}

export function usageFundTreeReceive(nodes, expandedIds) {
    return {
        area: types.FUND_TREE_AREA_USAGE,
        type: types.FUND_FUND_TREE_RECEIVE,
        nodes,
        expandedIds,
        expandedIdsExtension: [],
    };
}
