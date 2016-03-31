import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

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