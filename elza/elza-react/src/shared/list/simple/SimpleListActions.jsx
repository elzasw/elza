import {storeFromArea} from './../../utils';

export const REQUEST = 'list.simple.request';
export const RESPONSE = 'list.simple.response';
export const FILTER = 'list.simple.filter';
export const INVALIDATE = 'list.simple.invalidate';
export const SELECT_PARENT = 'list.simple.selectParent';
export const RESET = 'list.simple.reset';

/**
 * Zneplatnění dat.
 * @param area oblast
 * @param data podrobnosti
 * @returns {{type: string, area: *, data: *}}
 */
export function invalidate(area, data) {
    return {
        type: INVALIDATE,
        area,
        data,
    };
}

/**
 * Filtrování položek seznamu.
 * @param area oblast
 * @param filter filtr
 * @returns {{type: string, area: *, filter: *}}
 */
export function filter(area, filter) {
    return {
        type: FILTER,
        area,
        filter,
    };
}

/**
 * Reset store.
 * @param area oblast
 * @returns {{type: string, area: *, filter: *}}
 */
export function reset(area) {
    return {
        type: RESET,
        area,
    };
}

/**
 * Načtení dat pokud je potřeba.
 * @param area oblast
 * @param parent parent data pro seznam, pokud je potřeba, např. id nadřazené entity, ke které se vztahuje seznam
 * @param getData promise pro načtení, vstupem je parent a filtr
 * @param forceFetch pokud je true, provede se vždy nové načtení
 */
export function fetchIfNeeded(area, parent, getData, forceFetch = false) {
    return (dispatch, getState) => {
        // Spočtení data key
        var store = storeFromArea(getState(), area);

        // Pokud ve store není potřebné parent, dáme ho tam, aby se nám správně spočetl data key
        if (store.parent !== parent) {
            var same = false;
            if (typeof store.parent === 'object' && typeof parent === 'object') {
                // porovnáme objekty
                if (JSON.stringify(store.parent) === JSON.stringify(parent)) {
                    same = true;
                }
            }

            if (!same) {
                dispatch(selectParent(area, parent));
                store = storeFromArea(getState(), area);
            }
        }

        const dataKey = store.getDataKey.bind(store)();
        if (forceFetch || store.currentDataKey !== dataKey) {
            dispatch(request(area, dataKey));

            getData(store.parent, store.filter).then(json => {
                const newStore = storeFromArea(getState(), area);
                const newDataKey = newStore.getDataKey.bind(newStore)();
                if (newDataKey === dataKey) {
                    // jen pokud přídchozí objekt odpovídá datům, které chceme ve store
                    dispatch(response(area, json.rows, json.count));
                }
            }); // TODO [stanekpa] - řešit catch a vrácení datakey!
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
        case FILTER:
        case REQUEST:
        case RESPONSE:
        case INVALIDATE:
        case RESET:
            return true;
        default:
            return false;
    }
}

/**
 * Výběr nového parenta, volá se i v případě, že je zavoláno fetchIfNeeded s jiným parent něž ve store.
 * @param area oblast
 * @param parent parent
 * @returns {{type: string, area: *, parent: *}}
 */
function selectParent(area, parent) {
    return {
        type: SELECT_PARENT,
        area,
        parent,
    };
}

/**
 * Request dat.
 * @param area oblast
 * @param dataKey data key
 * @returns {{type: string, area: *, dataKey: *}}
 */
function request(area, dataKey) {
    return {
        type: REQUEST,
        area,
        dataKey,
    };
}

export function setData(area, parent, data) {
    return (dispatch, getState) => {
        // Načtení aktuálního store
        let store = storeFromArea(getState(), area);

        // Pokud ve store není potřebné id, dáme ho tam, aby se nám správně spočetl data key
        if (store.parent !== parent) {
            dispatch(selectParent(area, parent));
            store = storeFromArea(getState(), area);
        }

        const dataKey = store.getDataKey.bind(store)();

        // Simulace request dat
        dispatch(request(area, dataKey));

        // Response dat
        dispatch(response(area, data, data.length));
    };
}

/**
 * Response dat.
 * @param area oblast
 * @param rows řádky
 * @param count celkový počet řádek, např. v db pro daný filter
 * @returns {{type: string, area: *, data: *}}
 */
function response(area, rows, count) {
    return {
        type: RESPONSE,
        area,
        rows,
        count,
    };
}
