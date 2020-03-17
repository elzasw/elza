import * as types from 'actions/constants/ActionTypes.js';

export default function fundForm(state, action) {
    switch (action.type) {
        case types.GLOBAL_INIT_FORM_DATA:
            if (action.form === 'fundForm') {
                return {
                    ...state,
                    initialValues: action.data,
                };
            } else {
                return state;
            }
        case 'redux-form/CHANGE':
            switch (action.field) {
                case 'ruleSetId':
                    if (action.value === '') {
                        return {
                            ...state,
                            rulArrTypeId: {...state.rulArrTypeId, value: ''},
                        };
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
