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

export function structureNodeFormSelectId(versionId, id) {
    return {
        type: types.STRUCTURE_NODE_FORM_SELECT_ID,
        versionId,
        id
    }
}

function structureNodeFormRequest(versionId, id) {
    return {
        type: types.STRUCTURE_NODE_FORM_REQUEST,
        versionId,
        id
    }
}
function structureNodeFormReceive(versionId, data) {
    return {
        type: types.STRUCTURE_NODE_FORM_RECEIVE,
        versionId,
        data
    }
}


function _getArea(getState, versionId) {
    const state = getState();
    const index = indexById(state.arrRegion.funds, versionId, "versionId");
    if (index !== null) {
        const fund = state.arrRegion.funds[index];
        return fund.structureNodeForm;
    }
    return null;
}

/**
 * Fetch dat pro formulář struktur
 */
export function structureNodeFormFetchIfNeeded(versionId, id) {
    return (dispatch, getState) => {
        const storeArea = _getArea(getState, versionId);

        if (storeArea === null) {
            return
        }


        if (storeArea.currentDataKey !== id) {
            dispatch(structureNodeFormRequest(versionId, id));
            WebApi.getStructureData(versionId, id)
                .then(json => {
                    const newStoreArea = _getArea(getState, versionId);
                    if (newStoreArea === null) {
                        return;
                    }

                    if (json.id === id) {
                        dispatch(structureNodeFormReceive(versionId, json))
                    }
                })
        }
    }
}
