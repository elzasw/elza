import * as types from 'actions/constants/actionTypes';

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
