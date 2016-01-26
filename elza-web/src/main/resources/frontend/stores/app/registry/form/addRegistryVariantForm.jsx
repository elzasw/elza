import * as types from 'actions/constants/actionTypes';

export default function addRegistryVariantForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form == 'addRegistryVariantForm') {
                return {
                    ...state,
                    initialValues: action.data
                }
            } else {
                return state;
            }
        case "redux-form/CHANGE":
            switch (action.field) {
                case "registrySetId":
                    if (action.value == '') {
                        return {
                            ...state,
                            registryTypeId: {...state.registryTypeId, value: ''}
                    }
            } else {
            return state;
        }
        default:
            return state;
    }
    default:
    return state;
}
}

