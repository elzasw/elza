import * as types from 'actions/constants/ActionTypes';

const initialState = {
    visible: false,
    component: null,
    menu: null,
    position: null,
};

export default function contextMenu(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_CONTEXT_MENU_SHOW:
            return Object.assign({}, state, {
                visible: true,
                component: action.component,
                menu: action.menu,
                position: action.position,
            });
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            return Object.assign({}, state, {
                visible: false,
            });
        default:
            return state;
    }
}
