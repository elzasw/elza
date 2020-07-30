import * as types from '../../../actions/constants/ActionTypes';
import subNodeForm from '../arr/subNodeForm';
import {structureFormActions} from '../../../actions/arr/subNodeForm';
import {consolidateState} from '../../../components/Utils';
import {Action} from 'redux';

type State = {
    id: number | null;
    fetched: boolean;
    fetching: boolean;
    currentDataKey: any;
    subNodeForm: ReturnType<typeof subNodeForm>;
    version?: number;
};

const initialState: State = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    subNodeForm: subNodeForm(),
};

export interface StructureNodeFormAction extends Action {
    structureIds?: number[];
    id?: number;
    routingKey?: number;
    versionId?: number;
    data?: {
        [extraProps: string]: any;
    };
}

export default function structureNodeForm(
    state: State = initialState,
    action: StructureNodeFormAction = {} as any,
): State {
    if (structureFormActions.isSubNodeFormAction(action) || structureFormActions.isSubNodeFormCacheAction(action)) {
        const result = {
            ...state,
            subNodeForm: subNodeForm(state.subNodeForm, action),
        };
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.CHANGE_STRUCTURE:
            if (action.structureIds && action.structureIds.indexOf(state.id!) >= 0) {
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
                id: action.id!,
                version: action.versionId!,
                subNodeForm: subNodeForm(),
            };
        case types.STRUCTURE_NODE_FORM_REQUEST:
            return {
                ...state,
                subNodeForm:
                    state.subNodeForm.nodeId && state.subNodeForm.nodeId !== action.id
                        ? subNodeForm()
                        : state.subNodeForm,
                id: action.id!,
                fetching: true,
                fetched: false,
                currentDataKey: action.id,
            };
        case types.STRUCTURE_NODE_FORM_SET_DATA:
        case types.STRUCTURE_NODE_FORM_RECEIVE:
            return {
                ...state,
                id: action.id!,
                fetching: false,
                fetched: true,
                ...action.data,
            };
        default:
            return state;
    }
}
