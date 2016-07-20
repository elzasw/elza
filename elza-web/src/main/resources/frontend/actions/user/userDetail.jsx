import * as types from 'actions/constants/ActionTypes'
import {WebApi} from 'actions/index.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

export function userDetailChange(userDetail) {
    return {
        type: types.USER_DETAIL_CHANGE,
        userDetail,
    }
}

export function userDetailClear() {
    return {
        type: types.USER_DETAIL_CLEAR,
    }
}

export function userDetailsSaveSettings(settings) {
    return (dispatch) => {
        dispatch(userDetailRequestSettings(settings))
        WebApi.setUserSettings(settings)
            .then(data => {
                dispatch(userDetailResponseSettings(data));
                dispatch(modalDialogHide());
            });
    }
}

function userDetailRequestSettings(settings) {
    return {
        type: types.USER_DETAIL_REQUEST_SETTINGS,
        settings,
    }
}

function userDetailResponseSettings(settings) {
    return {
        type: types.USER_DETAIL_RESPONSE_SETTINGS,
        settings,
    }
}
