import * as types from 'actions/constants/ActionTypes.js';

export function statusSaving() {
    return {
        type: types.STATUS_SAVING,
    }
}
export function statusSaved() {
    return {
        type: types.STATUS_SAVED,
    }
}