import * as types from 'actions/constants/ActionTypes';
import {WebApi} from 'actions/index.jsx';
import objectById from '../../shared/utils/objectById';

export function isStructureNodeForm(action) {
    switch (action.type) {
        case types.CHANGE_STRUCTURE:
        case types.STRUCTURE_NODE_FORM_REQUEST:
        case types.STRUCTURE_NODE_FORM_RECEIVE:
        case types.STRUCTURE_NODE_FORM_SET_DATA:
        case types.STRUCTURE_NODE_FORM_SELECT_ID:
            return true;
        default:
            return false;
    }
}

export function structureNodeFormSelectId(versionId, id) {
    return {
        type: types.STRUCTURE_NODE_FORM_SELECT_ID,
        versionId,
        id,
    };
}

function structureNodeFormRequest(versionId, id) {
    return {
        type: types.STRUCTURE_NODE_FORM_REQUEST,
        versionId,
        id,
    };
}

function structureNodeFormReceive(versionId, data, id) {
    return {
        type: types.STRUCTURE_NODE_FORM_RECEIVE,
        versionId,
        data,
        id,
    };
}
export function structureNodeFormSetData(id, data) {
    return {
        type: types.STRUCTURE_NODE_FORM_SET_DATA,
        id,
        data,
    };
}

function _getArea(getState, id) {
    const state = getState();
    return state.structures.stores.hasOwnProperty(id) ? state.structures.stores[id] : null;
}

/**
 * Fetch dat pro formulář struktur
 */
export function structureNodeFormFetchIfNeeded(versionId, id) {
    return (dispatch, getState) => {
        const storeArea = _getArea(getState, id);

        if (storeArea === null) {
            return;
        }

        if (storeArea.currentDataKey !== id) {
            dispatch(structureNodeFormRequest(versionId, id));
            WebApi.getStructureData(versionId, id).then(json => {
                const newStoreArea = _getArea(getState, id);
                if (newStoreArea === null) {
                    return;
                }

                if (json.id === id) {
                    dispatch(structureNodeFormReceive(versionId, json, id));
                }
            });
        }
    };
}
