import * as types from 'actions/constants/ActionTypes';

var _setFocus = false

/**
 * Prvek nastavil focus, metoda pro informaci, že ho nastavil.
 */
export function focusWasSet() {
    _setFocus = false
}

export function isFocusFor(focusState, region, area = null, component = null, item = null) {
    if (region && focusState.region !== region) {
        return false;
    }
    if (area && focusState.area !== area) {
        return false;
    }
    if (component && focusState.component !== component) {
        return false;
    }
    if (item && focusState.item !== item) {
        return false;
    }
    return true;
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
