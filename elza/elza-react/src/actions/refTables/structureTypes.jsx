import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx';

export function structureTypesDataReceive(versionId, data) {
    return {
        type: types.REF_STRUCTURE_TYPES_RECEIVE,
        versionId,
        data,
    };
}

export function structureTypesFetching(versionId) {
    return {
        type: types.REF_STRUCTURE_TYPES_FETCHING,
        versionId,
    };
}

export function structureTypesDirty(versionId) {
    return {
        type: types.REF_STRUCTURE_TYPES_DIRTY,
        versionId,
    };
}

export function structureTypesFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        const state = getState().refTables.structureTypes;

        const index = indexById(state.data, versionId, 'versionId');
        if (index !== null) {
            if (state.data[index].isDirty && !state.data[index].isFetching) {
                dispatch(structureTypesRequest(versionId));
            }
        } else {
            dispatch(structureTypesFetching(versionId));
            dispatch(structureTypesRequest(versionId));
        }
    };
}

export function structureTypesRequest(versionId) {
    return dispatch => {
        WebApi.findRulStructureTypes(versionId).then(data => {
            dispatch(structureTypesDataReceive(versionId, data));
        });
    };
}
