/**
 * Akce pro kontextové menu.
 */

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Zobrazení menu.
 * @param {Object} component jaká komponenta menu chce zobrazit
 * @param {Object} menu obsah menu - <ul className="dropdown-menu"> a její obsah
 * @param {Object} position pozice
 */
export function contextMenuShow(component, menu, position={x:0,y:0}) {
    return {
        type: types.GLOBAL_CONTEXT_MENU_SHOW,
        component,
        menu,
        position,
    }
}

/**
 * Skrytí kontextového menu, pokud je vidět.
 */
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
