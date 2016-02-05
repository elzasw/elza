import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    isDirty: true,
    errors: [],
    count: 0
};

export default function versionValidation(state = initialState, action) {
    switch (action.type) {
        case types.CHANGE_CONFORMITY_INFO:
            return {
                ...state,
                isDirty: true
            };
        case types.FA_VERSION_VALIDATION_LOAD:
            return {
                ...state,
                isFetching: true
            };
        case types.FA_VERSION_VALIDATION_RECEIVED:
            return {
                ...state,
                ...action.data,
                isFetching: false,
                isDirty: false
            };
        default:
            return state
    }
}

