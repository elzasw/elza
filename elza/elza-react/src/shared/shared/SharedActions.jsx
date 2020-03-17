import {storeFromArea} from './../utils';

export const INIT_REDUCER = 'shared.initReducer';

/**
 * Inicializace shared store.
 * @param area oblast
 * @param store konkrétní store
 */
export function init(area, store) {
    return {
        type: INIT_REDUCER,
        area,
        store,
    };
}

export function initIfNeeded(area, redudcer) {
    return (dispatch, getState) => {
        const currentStore = storeFromArea(getState(), area, false);
        if (currentStore === null) {
            const newStore = redudcer();
            dispatch(init(area, newStore));
        }
    };
}

/**
 * Test, předaná akce je akcí pro tuto třídu.
 * @param action akce
 * @returns {boolean} true, pokud action odpovídá této třídě
 */
export function is(action) {
    switch (action.type) {
        case INIT_REDUCER:
            return true;
        default:
            return false;
    }
}
