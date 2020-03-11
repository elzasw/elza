import * as types from 'actions/constants/ActionTypes.js';
import subNodeForm from './subNodeForm.jsx';
import { structureFormActions } from 'actions/arr/subNodeForm.jsx';
import { consolidateState } from 'components/Utils.jsx';

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    subNodeForm: subNodeForm(),
};

export default function structureNodeForm(state = initialState, action = {}) {
    if (structureFormActions.isSubNodeFormAction(action) || structureFormActions.isSubNodeFormCacheAction(action)) {
        const result = {
            ...state,
            subNodeForm: subNodeForm(state.subNodeForm, action),
        };
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.CHANGE_STRUCTURE:
            if (action.structureIds && action.structureIds.indexOf(state.id) >= 0) {
                return {
                    ...state,
                    subNodeForm: subNodeForm(state.subNodeForm, action),
                };
            } else {
                return state;
            }
        case types.STRUCTURE_NODE_FORM_SELECT_ID:
            return {
                ...state,
                id: action.id,
                version: action.versionId,
                subNodeForm: subNodeForm(),
            };
        case types.STRUCTURE_NODE_FORM_REQUEST:
            return {
                ...state,
                subNodeForm: state.subNodeForm.nodeId && state.subNodeForm.nodeId !== action.id ? subNodeForm() : state.subNodeForm,
                id: action.id,
                fetching: true,
                fetched: false,
                currentDataKey: action.id,
            };
        case types.STRUCTURE_NODE_FORM_RECEIVE:
            return {
                ...state,
                id: action.id,
                fetching: false,
                fetched: true,
                ...action.data,
            };
        default:
            return state;
    }
}

