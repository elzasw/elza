import * as types from '../../../actions/constants/ActionTypes.js';
import {consolidateState} from '../../../components/Utils.jsx'
import {accessPointFormActions} from "../../../components/accesspoint/AccessPointFormActions";
import {itemForm} from "./itemForm";

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    form: itemForm()
};

export default function accessPoint(state = initialState, action = {}) {
    if (accessPointFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            form: itemForm(state.form, action),
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

