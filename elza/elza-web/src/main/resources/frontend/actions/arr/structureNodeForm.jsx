import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx'
import {i18n} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx';

export function isStructureNodeForm(action) {
    switch (action.type) {
        case types.CHANGE_STRUCTURE:
        case types.STRUCTURE_NODE_FORM_REQUEST:
        case types.STRUCTURE_NODE_FORM_RECEIVE:
        case types.STRUCTURE_NODE_FORM_SELECT_ID:
            return true;
        default:
            return false
    }
}

export function structureNodeFormSelectId(id) {
    return {
        type: types.STRUCTURE_NODE_FORM_SELECT_ID,
        id
    }
}

function structureNodeFormRequest(id) {
    return {
        type: types.STRUCTURE_NODE_FORM_REQUEST,
        id
    }
}
function structureNodeFormReceive(id, data) {
    return {
        type: types.STRUCTURE_NODE_FORM_RECEIVE,
        id,
        data
    }
}


function _getArea(getState, id) {
    const state = getState();
    const subStore = state.structures.stores[id];
    if (subStore !== null) {
        return subStore;
    }
    return null;
}

/**
 * Fetch dat pro formulář struktur
 */
export function structureNodeFormFetchIfNeeded(versionId, id, force = false) {
    return (dispatch, getState) => {
        const storeArea = _getArea(getState, id);

        if (storeArea === null) {
            return
        }

        if ((storeArea.currentDataKey !== id && !storeArea.fetching) || force) {
            dispatch(structureNodeFormRequest(id));
            WebApi.getStructureData(versionId, id)
                .then(json => {
                    const newStoreArea = _getArea(getState, id);
                    if (newStoreArea === null) {
                        return;
                    }

                    if (json.id === id) {
                        dispatch(structureNodeFormReceive(id, json))
                    }
                })
        }
    }
}
