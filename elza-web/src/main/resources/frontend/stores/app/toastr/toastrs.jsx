/**
 * 
 * Store pro toast notifikace
 * 
 */
import * as types from 'actions/constants/actionTypes';
import toastr from './toastr';

const initialState = {
    lastKey: 1,
    toastrs : [],
}

export default function toastrs(state = initialState, action) {
    switch (action.type) {
        case types.TOASTR_TYPE_WARNING:
        case types.TOASTR_TYPE_SUCCESS:
        case types.TOASTR_TYPE_INFO:
        case types.TOASTR_TYPE_DANGER:
            action.toastrId = action.lastKey;
            return Object.assign({}, state, {
                lastKey: state.lastKey+1,
                toastrs: [
                        ...state.toastrs,
// přepsat store jednoho toustu na objekt
                        toastr(state.toastr, action)
                    ]
            })
        case types.TOASTR_TYPE_CLEAR:
            
        default:
            return state
    }
}
