import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    saving: false,
    counter:0
};

export default function status(state = initialState, action = {}) {
    switch (action.type) {
        case types.STATUS_SAVED:
            return {
                ...state,
                saving: false,
                counter: state.counter-1 >= 0 ? state.counter-1 : 0
            }
        case types.STATUS_SAVING:
            return {
                ...state,
                saving: true,
                counter: state.counter+1
            }
        default:
            return state
    }
}
