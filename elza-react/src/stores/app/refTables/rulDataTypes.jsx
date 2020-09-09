import * as types from 'actions/constants/ActionTypes';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';

export default function rulDataTypes(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_RUL_DATA_TYPES_REQUEST, types.REF_RUL_DATA_TYPES_RECEIVE, state, action);
}
