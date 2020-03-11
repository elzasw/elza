import * as types from '../../../actions/constants/ActionTypes.js';
import {consolidateState} from '../../../components/Utils.jsx';
import {accessPointFormActions} from '../../../components/accesspoint/AccessPointFormActions';
import {itemForm} from './itemForm';
import {apNameFormActions} from '../../../components/accesspoint/ApNameFormActions';
import {fragmentItemFormActions} from '../../../components/accesspoint/FragmentItemFormActions';

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    form: itemForm(),
    nameItemForm: itemForm(),
    fragmentItemForm: itemForm()
};

export default function accessPoint(state = initialState, action = {}) {
    if (accessPointFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            form: itemForm(state.form, action),
        };
        return consolidateState(state, result);
    } else if (apNameFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            nameItemForm: itemForm(state.nameItemForm, action),
        };
        return consolidateState(state, result);
    } else if (fragmentItemFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            fragmentItemForm: itemForm(state.fragmentItemForm, action),
        };
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.STORE_SAVE:{
            const {id} = state;
            return {
                id
            }
        }
        case types.STORE_LOAD:{
            return {
                ...state,
                fetched: false,
                fetching: false,
                currentDataKey: '',
                form: itemForm()
            }
        }
        default:
            return state
    }
}

