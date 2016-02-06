import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

export function versionValidate(versionId, loadErrors = false) {
    return (dispatch) => {
        dispatch(versionValidationLoad(versionId));
        if (!loadErrors) {
            WebApi.versionValidateCount(versionId).then(function (count) {
                dispatch(versionValidationRecieved(versionId, {
                    count: count,
                }));
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
                    dispatch(versionValidationRecieved(versionId, {
                        count: json.count,
                        errors: json.errors
                    }));
                });
        }
    }
}

export function versionValidationLoad(versionId) {
    return {
        versionId,
        type: types.FA_VERSION_VALIDATION_LOAD
    }
}

export function versionValidationRecieved(versionId, data) {
    return {
        versionId,
        type: types.FA_VERSION_VALIDATION_RECEIVED,
        data
    }
}