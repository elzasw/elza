import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    saveCounter:0
};

export default function status(state = initialState, action = {}) {
    switch (action.type) {
        case types.STATUS_SAVED:
            return {
                ...state,
                saving: false,
                saveCounter: state.saveCounter-1 >= 0 ? state.saveCounter-1 : 0
            }
        case types.STATUS_SAVING:
            return {
                ...state,
                saveCounter: state.saveCounter+1
            }
        default:
            return state
    }
}
