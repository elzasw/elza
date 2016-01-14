/**
 * 
 * Store pro toast notifikace
 * 
 **/ 
import * as types from 'actions/constants/actionTypes';


const initialState = {
    toastrId: null,
    title: null,
    message: null,
    type: null,
    dismissAfter: null
}

export default function toastr(state = initialState, action) {
    switch (action.type) {
        case types.TOASTR_TYPE_INFO:
            return Object.assign({}, state, {
                dismissAfter: 2000,
                toastrId: action.toastrId,
                title: action.title,
                message: action.message,
                type: action.type
            })
        case types.TOASTR_TYPE_SUCCESS:
            return Object.assign({}, state, {
                dismissAfter: 2000,
                toastrId: action.toastrId,
                title: action.title,
                message: action.message,
                type: action.type
            })
        case types.TOASTR_TYPE_WARNING:
            return Object.assign({}, state, {
                toastrId: action.toastrId,
                title: action.title,
                message: action.message,
                type: action.type
            })
        case types.TOASTR_TYPE_DANGER:
            return Object.assign({}, state, {
                toastrId: action.toastrId,
                title: action.title,
                message: action.message,
                type: action.type
            })
        default:
            return state
    }
}
