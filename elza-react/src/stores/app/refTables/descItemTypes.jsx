import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';

export default function descItemTypes(state = genericRefTableState, action = {}) {
    switch (action.type) {
        case types.CHANGE_PACKAGE:{
            return {
                ...state,
                dirty: true
            }
        }
        default:
            return genericRefTable(types.REF_DESC_ITEM_TYPES_REQUEST, types.REF_DESC_ITEM_TYPES_RECEIVE, state, action)
    }
}
