import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

export function versionValidate(versionId, loadErrors = false) {
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
                WebApi.versionValidate(versionId)
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
                        errors: json.errors
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