import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from "./genericRefTable";

export default function calendarTypes(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_CALENDAR_TYPES_REQUEST, types.REF_CALENDAR_TYPES_RECEIVE, state, action);
}
