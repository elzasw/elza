import * as types from 'actions/constants/ActionTypes'

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
