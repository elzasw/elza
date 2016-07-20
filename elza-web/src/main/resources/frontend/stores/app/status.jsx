import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    saving: false,
}

export default function status(state = initialState, action = {}) {
    switch (action.type) {
        case types.STATUS_SAVED:
            return {
                ...state,
                saving: false
            }
        case types.STATUS_SAVING:
            return {
                ...state,
                saving: true
            }
        default:
            return state
    }
}
