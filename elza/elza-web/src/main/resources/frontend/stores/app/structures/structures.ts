import * as types from '../../../actions/constants/ActionTypes.js';
import {structureFormActions} from '../../../actions/arr/subNodeForm.jsx'
import {consolidateState} from '../../../components/Utils.jsx'
import {default as structureNodeForm, StructureNodeFormAction} from "./structureNodeForm";

type State = {
    stores: {
        [key: number]: ReturnType<typeof structureNodeForm>;
    };
}

const initialState: State = {
    stores: {
        [-1]: structureNodeForm(),
    }
};
const xx = (action) => {
    switch (action.type) {
        case types.STRUCTURE_NODE_FORM_REQUEST:
        case types.STRUCTURE_NODE_FORM_RECEIVE:
            return true;
    }
    return false;
}

export default function sturctures(state = initialState, action: StructureNodeFormAction) {
    if (structureFormActions.isSubNodeFormAction(action) || structureFormActions.isSubNodeFormCacheAction(action)) {
        if (action.routingKey && state.stores.hasOwnProperty(action.routingKey)){
            const result = {
                ...state,
                stores: {
                    ...state.stores,
                    [action.routingKey]: structureNodeForm(state.stores[action.routingKey], action)
                }
            };
            return consolidateState(state, result);
        } else {
            console.warn("missing actionId or store in structures");
            return state;
        }
    }


    switch (action.type) {
        case types.STRUCTURE_NODE_FORM_SELECT_ID:
            if (action.id && !state.stores.hasOwnProperty(action.id)) {
                return {
                    ...state,
                    stores: {
                        ...state.stores,
                        [action.id]: structureNodeForm(structureNodeForm(), action)
                    }
                };
            }
        case types.STRUCTURE_NODE_FORM_REQUEST:
        case types.STRUCTURE_NODE_FORM_RECEIVE: {
            if (action.id && state.stores.hasOwnProperty(action.id)) {
                return {
                    ...state,
                    stores: {
                        ...state.stores,
                        [action.id]: structureNodeForm({...state.stores[action.id]}, action)
                    }
                };
            } else {
                console.warn("missing actionId or store in structures");
                return state;
            }
        }
        default:
            return state
    }
}

