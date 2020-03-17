import * as types from 'actions/constants/ActionTypes.js';

export default function addRegistryForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form === 'addRegistryForm') {
                return {
                    ...state,
                    initialValues: action.data,
                };
            } else {
                return state;
            }

        default:
            return state;
    }
}
