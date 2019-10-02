import * as types from 'actions/constants/ActionTypes.js';

export function routerNavigate(path) {
    return {
        type: types.ROUTER_NAVIGATE,
        path
    }
}
export function routerNavigateFinish() {
    return {
        type: types.ROUTER_NAVIGATE_CLEAR,
    }
}
