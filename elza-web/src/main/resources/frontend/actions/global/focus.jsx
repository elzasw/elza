import * as types from 'actions/constants/ActionTypes';

var _setFocus = false

/**
 * Prvek nastavil focus, metoda pro informaci, že ho nastavil.
 */
export function focusWasSet() {
    _setFocus = false
}

/**
 * Test, zda se může focus nastavit.
 */
export function canSetFocus() {
    return _setFocus
}

/**
 * Vyvolání akce pro nastavení focusu.
 */
export function setFocus(region, area, component, item) {
    _setFocus = true;

    return {
        type: types.SET_FOCUS,
        region,
        area,
        component,
        item
    }
}
