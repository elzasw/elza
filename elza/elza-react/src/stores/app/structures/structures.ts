import * as types from '../../../actions/constants/ActionTypes';
import {structureFormActions} from 'actions/arr/subNodeForm';
import {consolidateState} from '../../../components/Utils.jsx';
import {default as structureNodeForm, StructureNodeFormAction} from './structureNodeForm';

type State = {
    stores: {
        [key: number]: ReturnType<typeof structureNodeForm>;
    };
};

const initialState: State = {
    stores: {
        [-1]: structureNodeForm(),
    },
};

export default function sturctures(state = initialState, action: StructureNodeFormAction) {
    if (structureFormActions.isSubNodeFormAction(action) || structureFormActions.isSubNodeFormCacheAction(action)) {
        if (action.routingKey && state.stores.hasOwnProperty(String(action.routingKey))) {
            const result = {
                ...state,
                stores: {
                    ...state.stores,
                    [String(action.routingKey)]: structureNodeForm(state.stores[String(action.routingKey)], action),
                },
            };
            return consolidateState(state, result);
        } else {
            console.warn('missing actionId or store in structures', action, state);
            return state;
        }
    }

    switch (action.type) {
        case types.CHANGE_STRUCTURE:
            const modifiedStores = {};
            if (action.structureIds) {
                for (let id of action.structureIds!) {
                    if (state.stores.hasOwnProperty(id)) {
                        modifiedStores[id] = structureNodeForm(state.stores[id], action);
                    }
                }
                return {
                    ...state,
                    stores: {
                        ...state.stores,
                        ...modifiedStores,
                    },
                };
            } else {
                return state;
            }
        case types.STRUCTURE_NODE_FORM_SELECT_ID:
            if (action.id && !state.stores.hasOwnProperty(action.id)) {
                return {
                    ...state,
                    stores: {
                        ...state.stores,
                        [action.id]: structureNodeForm(structureNodeForm(), action),
                    },
                };
            } else {
                return state;
            }
        case types.STRUCTURE_NODE_FORM_SET_DATA:
        case types.STRUCTURE_NODE_FORM_REQUEST:
        case types.STRUCTURE_NODE_FORM_RECEIVE: {
            if (action.id && state.stores.hasOwnProperty(action.id)) {
                return {
                    ...state,
                    stores: {
                        ...state.stores,
                        [action.id]: structureNodeForm({...state.stores[action.id]}, action),
                    },
                };
            } else {
                console.warn('missing actionId or store in structures', action, state);
                return state;
            }
        }
        default:
            return state;
    }
}
