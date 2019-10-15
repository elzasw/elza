import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {barrier} from 'components/Utils.jsx';
import {fundSelectSubNode} from 'actions/arr/node.jsx'
import {i18n} from 'components/shared';
import {addToastrInfo} from 'components/shared/toastr/ToastrActions.jsx'

export function isVersionValidation(action) {
    switch (action.type) {
        case types.FUND_VERSION_VALIDATION_LOAD:
        case types.FUND_VERSION_VALIDATION_RECEIVED:
            return true
        default:
            return false
    }
}

export function versionValidate(versionId, loadErrors = false, showAll = false) {
    return (dispatch) => {
        dispatch(versionValidationLoad(versionId));
        if (!loadErrors) {
            WebApi.versionValidateCount(versionId).then(function (count) {
                dispatch(versionValidationReceived(versionId, {
                    count: count,
                }, true));
            });
        } else {
            barrier(
                WebApi.versionValidateCount(versionId),
                WebApi.versionValidate(versionId, showAll)
            )
                .then(data => {
                    return {
                        count: data[0].data,
                        errors: data[1].data
                    }
                })
                .then(json => {
                    dispatch(versionValidationReceived(versionId, {
                        count: json.count,
                        errors: json.errors,
                        showAll: showAll
                    }, false));
                });
        }
    }
}

export function versionValidationLoad(versionId) {
    return {
        versionId,
        type: types.FUND_VERSION_VALIDATION_LOAD
    }
}

export function versionValidationReceived(versionId, data, isErrorListDirty) {
    return {
        versionId,
        type: types.FUND_VERSION_VALIDATION_RECEIVED,
        data,
        isErrorListDirty
    }
}

export function versionValidationErrorNext(versionId, nodeId) {
    return (dispatch) => {
        dispatch(versionValidationErrorNextRequest(versionId, nodeId));
        WebApi.findValidationError(versionId, nodeId, 1).then(function (data) {
            if (data.items !== null && data.count > 0) {
                var node = data.items[0];
                dispatch(versionValidationErrorNextReceive(versionId, node));
                dispatch(fundSelectSubNode(versionId, node.id, node.parentNode));
            } else {
                dispatch(addToastrInfo(i18n('toast.arr.validation.error.notFound')));
            }
        });
    }
}

export function versionValidationErrorNextRequest(versionId, nodeId) {
    return {
        type: types.FUND_VERSION_VALIDATION_ERROR_NEXT_REQUEST,
        versionId,
        nodeId
    }
}

export function versionValidationErrorNextReceive(versionId, node) {
    return {
        type: types.FUND_VERSION_VALIDATION_ERROR_NEXT_RECEIVE,
        versionId,
        node
    }
}

export function versionValidationErrorPrevious(versionId, nodeId) {
    return (dispatch, getState) => {
        dispatch(versionValidationErrorPreviousRequest(versionId, nodeId));
        WebApi.findValidationError(versionId, nodeId, -1).then(function (data) {
            if (data.items !== null && data.count > 0) {
                var node = data.items[0];
                dispatch(versionValidationErrorPreviousReceive(versionId, node));
                dispatch(fundSelectSubNode(versionId, node.id, node.parentNode));
            } else {
                dispatch(addToastrInfo(i18n('toast.arr.validation.error.notFound')));
            }
        });
    }
}

export function versionValidationErrorPreviousRequest(versionId, nodeId) {
    return {
        type: types.FUND_VERSION_VALIDATION_ERROR_PREVIOUS_REQUEST,
        versionId,
        nodeId
    }
}

export function versionValidationErrorPreviousReceive(versionId, node) {
    return {
        type: types.FUND_VERSION_VALIDATION_ERROR_PREVIOUS_RECEIVE,
        versionId,
        node
    }
}
