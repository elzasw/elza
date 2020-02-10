import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    isErrorListDirty: true,
    isFetching: false,
    isDirty: true,
    errors: [],
    showAll: false,
    count: 0
};

export default function versionValidation(state = initialState, action) {
    switch (action.type) {
        case types.CHANGE_CONFORMITY_INFO:
            return {
                ...state,
                isDirty: true
            };
        case types.FUND_VERSION_VALIDATION_LOAD:
            return {
                ...state,
                isFetching: true
            };
        case types.FUND_VERSION_VALIDATION_RECEIVED:
            return {
                ...state,
                ...action.data,
                isFetching: false,
                isDirty: false,
                isErrorListDirty: action.isErrorListDirty
            };
        default:
            return state
    }
}

