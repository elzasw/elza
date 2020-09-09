import * as types from 'actions/constants/ActionTypes';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';

export default function institutions(state = genericRefTableState, action = {}) {
    switch (action.type) {
        case types.CHANGE_INSTITUTION: {
            return {
                ...state,
                dirty: true,
            };
        }
        default:
            return genericRefTable(types.REF_INSTITUTIONS_REQUEST, types.REF_INSTITUTIONS_RECEIVE, state, action);
    }
}
