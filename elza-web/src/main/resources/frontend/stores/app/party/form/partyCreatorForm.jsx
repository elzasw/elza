import * as types from 'actions/constants/ActionTypes.js';

export default function partyCreatorForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form == 'partyCreatorForm') {
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
