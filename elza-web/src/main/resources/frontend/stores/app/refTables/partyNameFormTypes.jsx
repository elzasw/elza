import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from "./genericRefTable";

export default function partyNameFormTypes(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_PARTY_NAME_FORM_TYPES_REQUEST, types.REF_PARTY_NAME_FORM_TYPES_RECEIVE, state, action);
}
