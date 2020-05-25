import * as types from "../../../actions/constants/ActionTypes";
import {default as genericRefTable, genericRefTableState} from "./genericRefTable";

export default function apTypes(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_AP_TYPES_REQUEST, types.REF_AP_TYPES_RECEIVE, state, action);
}
