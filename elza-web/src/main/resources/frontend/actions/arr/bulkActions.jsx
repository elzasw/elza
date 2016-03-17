import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

export function isBulkAction(action) {
    switch (action.type) {
        case types.BULK_ACTIONS_STATE_CHANGE:
        case types.BULK_ACTIONS_DATA_LOADING:
        case types.BULK_ACTIONS_RECEIVED_DATA:
        case types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA:
        case types.BULK_ACTIONS_RECEIVED_STATE:
        case types.BULK_ACTIONS_STATE_IS_DIRTY:
            return true
        default:
            return false
    }
}

export function bulkActionsLoadData(versionId, mandatory = false, silent = false) {
    return (dispatch) => {
        if (!silent) {
            dispatch(bulkActionsDataLoading(versionId, mandatory));
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
                dispatch(bulkActionsDataReceived(versionId, { actions: json.actions, states: json.states }, mandatory));
            });
    }
}

export function bulkActionsValidateVersion(versionId, silent = false) {
    return (dispatch) => {

        if (!silent) {
            dispatch(bulkActionsDataLoading(versionId, true));
        }
        barrier(
            WebApi.bulkActionValidate(versionId),
            WebApi.getBulkActionsState(versionId)
        )
            .then(data => {
                return {
                    actions: data[0].data,
                    states: data[1].data
                }
            })
            .then(json => {
                dispatch(bulkActionsVersionValidateDataReceived(versionId, { actions: json.actions, states: json.states }, true));
            });
    }
}

export function bulkActionsRun(versionId, code) {
    return (dispatch) => {
        dispatch(bulkActionsStateIsDirty(versionId, code));
        WebApi.bulkActionRun(versionId, code).then((result) => {
            dispatch(bulkActionsStateReceived(versionId, result, code))
        })
    }
}

export function bulkActionsDataReceived(versionId, data, mandatory) {
    return {
        type: types.BULK_ACTIONS_RECEIVED_DATA,
        versionId,
        data,
        mandatory
    }
}

export function bulkActionsVersionValidateDataReceived(versionId, data, mandatory) {
    return {
        type: types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA,
        versionId,
        data,
        mandatory
    }
}

export function bulkActionsStateReceived(versionId, data, code) {
    return {
        type: types.BULK_ACTIONS_RECEIVED_STATE,
        data,
        code
    }
}

export function bulkActionsDataLoading(versionId, mandatory) {
    return {
        type: types.BULK_ACTIONS_DATA_LOADING,
        mandatory,
        versionId
    }
}

export function buklActionStateChange(data) {
    return {
        type: types.BULK_ACTIONS_STATE_CHANGE,
        ...data
    }
}

export function bulkActionsStateIsDirty(versionId, code) {
    return {
        type: types.BULK_ACTIONS_STATE_IS_DIRTY,
        code
    }
}
