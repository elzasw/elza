import * as types from 'actions/constants/ActionTypes'
import {WebApi} from 'actions/index.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

export function userDetailChange(userDetail) {
    return {
        type: types.USER_DETAIL_CHANGE,
        userDetail,
    }
}

export function reloadUserDetail(userIds) {
    return (dispatch, getState) => {
        const state = getState();
        const userDetail = state.userDetail;
        if (userIds.indexOf(userDetail.id) !== -1) {
            WebApi.getUserDetail()
                .then(userDetail => {
                    dispatch(userDetailChange(userDetail))
                });
        }
    }
}

export function userDetailClear() {
    return {
        type: types.USER_DETAIL_CLEAR,
    }
}

/**
 * @param settings nastavení
 * @param hideDialog uzavřít dialog po úspěšném dokončení?
 */
export function userDetailsSaveSettings(settings, hideDialog = true) {
    return (dispatch) => {
        dispatch(userDetailRequestSettings(settings))
        WebApi.setUserSettings(settings)
            .then(data => {
                dispatch(userDetailResponseSettings(data));
                if (hideDialog === true) {
                    dispatch(modalDialogHide());
                }
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
