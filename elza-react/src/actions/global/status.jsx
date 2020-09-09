import * as types from 'actions/constants/ActionTypes';

export function savingApiWrapper(dispatch, apiPromise) {
    dispatch(statusSaving());

    apiPromise
        .then(result => {
            dispatch(statusSaved());
        })
        .catch(err => {
            dispatch(statusSaved());
        });

    return apiPromise;
}

export function statusSaving() {
    return {
        type: types.STATUS_SAVING,
    };
}
export function statusSaved() {
    return {
        type: types.STATUS_SAVED,
    };
}
