/**
 * akce pro toastry
 */


import * as types from 'actions/constants/ActionTypes';



export function toastrInfo(title, message = null) {
    return dispatch => {
        return {
            type: types.TOASTR_TYPE_INFO,
            title: title,            
            message: message,
        }
    }
}

export function toastrSuccess(title, message = null) {
    return dispatch => {
        return {
            type: types.TOASTR_TYPE_SUCCESS,
            title: title,            
            message: message,
        }
    }
}

export function toastrWarning(title, message = null) {
    return dispatch => {
        return {
            type: types.TOASTR_TYPE_WARNING,
            title: title,            
            message: message,
        }
    }
}

export function toastrDanger(title, message = null) {
    return dispatch => {
        return {
            type: types.TOASTR_TYPE_DANGER,
            title: title,            
            message: message,
        }
    }
}

export function clearToastr(toastrId) {
    return dispatch => {
        return {
            type: types.TOASTR_TYPE_CLEAR,
            toastrId: toastrId,
        }
    }
}




