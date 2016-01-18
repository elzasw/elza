import * as types from 'actions/constants/actionTypes';

export function contextMenuShow(component, menu, position={x:0,y:0}) {
    return {
        type: types.GLOBAL_CONTEXT_MENU_SHOW,
        component,
        menu,
        position,
    }
}
export function contextMenuHide() {
    return (dispatch, getState) => {
        var state = getState();
        if (state.contextMenu.visible) {
            return dispatch({
                type: types.GLOBAL_CONTEXT_MENU_HIDE,
            })
        }
    }
}
