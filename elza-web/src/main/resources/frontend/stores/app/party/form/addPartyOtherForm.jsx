import * as types from 'actions/constants/actionTypes';

export default function addPartyOtherForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form == 'addPartyOtherForm') {
                return {
                    ...state,
                    initialValues: action.data
                }
            } else {
                return state;
            }
        case "redux-form/CHANGE":
            switch (action.field) {
                default:
                  return state;
            }
        default:
            return state;
    }
}
