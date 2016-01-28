import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    navigateTo: null,
}

export default function router(state = initialState, action) {
    switch (action.type) {
        case types.ROUTER_NAVIGATE:
            return {
                ...state,
                navigateTo: action.path
            }
        case types.ROUTER_NAVIGATE_CLEAR:
            return {
                ...state,
                navigateTo: null
            }
        default:
            return state
    }
}
