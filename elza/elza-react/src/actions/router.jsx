import * as types from 'actions/constants/ActionTypes';

export function routerNavigate(path, method = 'PUSH') {
    return {
        type: types.ROUTER_NAVIGATE,
        method,
        path,
    };
}
export function routerNavigateFinish() {
    return {
        type: types.ROUTER_NAVIGATE_CLEAR,
    };
}
