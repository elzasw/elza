import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';

const initialState = {
    ...genericRefTableState,
    items: {}
};

export default function visiblePolicyTypes(state = initialState, action = {}) {
    return genericRefTable(types.REF_VISIBLE_POLICY_TYPES_REQUEST, types.REF_VISIBLE_POLICY_TYPES_RECEIVE, state, action)
}
