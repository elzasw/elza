import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';


export default function externalSystems(state = genericRefTableState, action = {}) {
    switch (action.type) {
        case types.REF_EXTERNAL_SYSTEMS_INVALID: {
            return {
                dirty: true
            }
        }
        default:
            return genericRefTable(types.REF_EXTERNAL_SYSTEMS_REQUEST, types.REF_EXTERNAL_SYSTEMS_RECEIVE, state, action);
    }
}
