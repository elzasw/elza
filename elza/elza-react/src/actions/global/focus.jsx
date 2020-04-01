/**
 * Akce pro nastavení focusu na různé prvky.
 * Focus funguje následovně: ve store je informace, kam se má focus nastavit, jestli se má ale opravdu nastavit je řízeno globální proměnnou _setFocus,
 * je to z důvodu, aby po nastavení focusu nebyl měněn store a nepřekreslovaly se komponenty.
 */

import * as types from 'actions/constants/ActionTypes.js';

// Vnitřní proměnná, které určuje, zda je nutné focus nastavit
var _setFocus = false;

/**
 * Prvek nastavil focus, metoda pro informaci, že ho nastavil.
 */
export function focusWasSet() {
    _setFocus = false;
}

/**
 * Test, zda je pro předané parametry focus určen. Pokud jsou některé parametry null, nejsou pro test uvažovány.
 */
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
 * Test, zda je pro předané parametry focus určen, musí PŘESNĚ odpovídat předaným parametrům včetně např. null hodnot atp.
 */
export function isFocusExactFor(focusState, region, area, component, item) {
    if (focusState.region !== region) {
        return false;
    }
    if (focusState.area !== area) {
        return false;
    }
    if (focusState.component !== component) {
        return false;
    }
    if (focusState.item !== item) {
        return false;
    }
    return true;
}

/**
 * Test, zda se může focus nastavit.
 */
export function canSetFocus() {
    return _setFocus;
}

/**
 * Vyvolání akce pro nastavení focusu.
 * @param {Object} region region viz. constant.jsx FOCUS_KEYS
 * @param {Object} area oblast v regionu, např. 0, 1, 2, 3, atp.
 * @param {Object} component komponenta, např. 'tree', 'list', atp.
 * @param {Object} item doplňující identifikace v komponentě, většinou objekt s dalšími atributy, např. {descItemTypeId: 123}, atp.
 */
export function setFocus(region, area, component = null, item = null) {
    _setFocus = true;
    return {
        type: types.SET_FOCUS,
        region,
        area,
        component,
        item,
    };
}
