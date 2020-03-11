import * as types from 'actions/constants/ActionTypes';
import { WebApi } from 'actions/index.jsx';

export function userDetailChange(userDetail) {
    return {
        type: types.USER_DETAIL_CHANGE,
        userDetail,
    };
}

export function reloadUserDetail(userIds) {
    return (dispatch, getState) => {
        const state = getState();
        const userDetail = state.userDetail;
        if (userIds.indexOf(userDetail.id) !== -1) {
            WebApi.getUserDetail()
                  .then(userDetail => {
                      dispatch(userDetailChange(userDetail));
                  });
        }
    };
}

export function userDetailClear() {
    return {
        type: types.USER_DETAIL_CLEAR,
    };
}

export function userDetailRequest() {
    return {
        type: types.USER_DETAIL_REQUEST,
    };
}

/**
 * @param settings nastavení
 * @param hideDialog uzavřít dialog po úspěšném dokončení?
 */
export function userDetailsSaveSettings(settings) {
    return (dispatch) => {
        dispatch(userDetailRequestSettings(settings));
        return WebApi.setUserSettings(settings)
                     .then(data => {
                         dispatch(userDetailResponseSettings(data));
                     });
    };
}

function userDetailRequestSettings(settings) {
    return {
        type: types.USER_DETAIL_REQUEST_SETTINGS,
        settings,
    };
}

function userDetailResponseSettings(settings) {
    return {
        type: types.USER_DETAIL_RESPONSE_SETTINGS,
        settings,
    };
}
