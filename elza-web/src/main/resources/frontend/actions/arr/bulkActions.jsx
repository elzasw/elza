import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

export function bulkActionsLoadData(versionId, mandatory = false, silent = false) {
    return (dispatch) => {
        if (!silent) {
            dispatch(bulkActionsDataLoading(mandatory));
        }
        barrier(
            WebApi.getBulkActions(versionId, mandatory),
            WebApi.getBulkActionsState(versionId)
        )
            .then(data => {
                return {
                    actions: data[0].data,
                    states: data[1].data
                }
            })
            .then(json => {
                dispatch(bulkActionsDataReceived({
                    actions: json.actions,
                    states: json.states
                }, mandatory));
            });
    }
}

export function bulkActionsValidateVersion(versionId, silent = false) {
    return (dispatch) => {
        if (!silent) {
            dispatch(bulkActionsDataLoading(true));
        }
        WebApi.bulkActionValidate(versionId).then((result) => {
            dispatch(bulkActionsVersionValidateDataReceived(result))
        });
    }
}

export function bulkActionsRun(versionId, code) {
    return (dispatch) => {
        dispatch(bulkActionsStateIsDirty(code));
        WebApi.bulkActionRun(versionId, code).then((result) => {
            dispatch(bulkActionsStateReceived(result, code))
        })
    }
}

export function bulkActionsDataReceived(data, mandatory) {
    return {
        type: types.BULK_ACTIONS_RECEIVED_DATA,
        data,
        mandatory
    }
}

export function bulkActionsVersionValidateDataReceived(data) {
    return {
        type: types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA,
        data
    }
}

export function bulkActionsStateReceived(data, code) {
    return {
        type: types.BULK_ACTIONS_RECEIVED_STATE,
        data,
        code
    }
}

export function bulkActionsDataLoading(mandatory) {
    return {
        type: types.BULK_ACTIONS_DATA_LOADING,
        mandatory
    }
}

export function buklActionStateChange(data) {
    return {
        type: types.BULK_ACTIONS_STATE_CHANGE,
        ...data
    }
}

export function bulkActionsStateIsDirty(code) {
    return {
        type: types.BULK_ACTIONS_STATE_IS_DIRTY,
        code
    }
}
