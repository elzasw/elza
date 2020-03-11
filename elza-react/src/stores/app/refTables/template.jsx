import * as types from 'actions/constants/ActionTypes.js';
import {default as genericRefTable, genericRefTableState} from './genericRefTable';


export default function template(state = genericRefTableState, action = {}) {
    return genericRefTable(types.REF_TEMPLATES_REQUEST, types.REF_TEMPLATES_RECEIVE, state, action)
}
