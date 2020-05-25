import * as types from "../../../actions/constants/ActionTypes";
import {default as genericRefTable, genericRefTableState} from "./genericRefTable";

export default function partTypes(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_PART_TYPES_REQUEST, types.REF_PART_TYPES_RECEIVE, state, action);
}
