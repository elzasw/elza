import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from "./genericRefTable";

export default function ruleSet(state = genericRefTableState, action = {}) {
    switch (action.type) {
        case types.CHANGE_PACKAGE:{
            return {
                dirty: true
            }
        }
        default:
            return genericRefTable(types.REF_RULE_SET_REQUEST, types.REF_RULE_SET_RECEIVE, state, action)
    }
}
