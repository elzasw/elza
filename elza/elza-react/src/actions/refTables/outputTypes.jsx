/**
 * Akce pro číselníky typů obalů.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

function getDataKey(getState) {
    const state = getState();
    return state.arrRegion.funds[state.arrRegion.activeIndex].versionId;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function outputTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        const {
            refTables: {outputTypes},
        } = getState();
        const dataKey = getDataKey(getState);
        if (
            dataKey !== outputTypes.currentDataKey ||
            ((!outputTypes.fetched || outputTypes.dirty) && !outputTypes.isFetching)
        ) {
            return dispatch(outputTypesFetch(dataKey));
        }
    };
}

/**
 * Nové načtení dat.
 */
export function outputTypesFetch(dataKey) {
    return dispatch => {
        dispatch(outputTypesRequest(dataKey));
        return WebApi.getOutputTypes(dataKey).then(data => dispatch(outputTypesReceive(data, dataKey)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} items objekt s daty
 * @param dataKey data key
 */
export function outputTypesReceive(items, dataKey) {
    return {
        type: types.REF_OUTPUT_TYPES_RECEIVE,
        items,
        dataKey,
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function outputTypesRequest(dataKey) {
    return {
        type: types.REF_OUTPUT_TYPES_REQUEST,
        dataKey,
    };
}
