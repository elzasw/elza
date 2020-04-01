import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    region: null,
    area: null,
    component: null,
    item: null,
};

export default function focus(state = initialState, action) {
    switch (action.type) {
        case types.SET_FOCUS:
            return {
                ...state,
                region: action.region,
                area: action.area,
                component: action.component,
                item: action.item,
            };
        default:
            return state;
    }
}
