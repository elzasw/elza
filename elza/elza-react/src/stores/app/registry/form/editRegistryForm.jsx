import * as types from 'actions/constants/ActionTypes';

export default function editRegistryForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form === 'editRegistryForm') {
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
