import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx';

const bulkActionsInitState = {
    isDirty: true,
    isFetching: false,
    actions: [],
    states: [],
    mandatory: false,
};

export default function bulkActions(state = bulkActionsInitState, action) {
    switch (action.type) {
        case types.BULK_ACTIONS_DATA_LOADING:
            return {...state, isFetching: true, mandatory: action.mandatory};
        case types.BULK_ACTIONS_RECEIVED_DATA:
            return {
                ...state,
                actions: action.data.actions,
                states: action.data.states !== undefined ? action.data.states : [],
                isFetching: false,
                isDirty: false,
                mandatory: action.mandatory,
            };
        case types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA:
            return {
                ...state,
                actions: action.data.actions !== undefined ? action.data.actions : [],
                states: action.data.states !== undefined ? action.data.states : [],
                isFetching: false,
                isDirty: false,
                mandatory: action.mandatory,
            };
        case types.BULK_ACTIONS_STATE_IS_DIRTY:
            var index = indexById(state.states, action.code, 'code');
            if (index == null) {
                return state;
            }
            return {
                ...state,
                states: [
                    ...state.states.slice(0, index),
                    {...state.states[index], isDirty: true},
                    ...state.states.slice(index + 1),
                ],
            };
        case types.BULK_ACTIONS_STATE_CHANGE:
            if (indexById(state.states, action.entityId, 'code') == null) {
                return state;
            }
            return {
                ...state,
                isDirty: true,
                isFetching: false,
            };
        case types.CHANGE_CONFORMITY_INFO:
            return {
                ...state,
                isDirty: true,
            };
        default:
            return state;
    }
}
