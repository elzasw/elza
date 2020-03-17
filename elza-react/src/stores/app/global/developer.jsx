import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    enabled: false,
};

export default function developer(state = initialState, action) {
    switch (action.type) {
        case types.DEVELOPER_SET:
            return {
                ...state,
                enabled: action.enabled,
            };
        default:
            return state;
    }
}
