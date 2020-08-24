import {storeFromArea} from './../utils';

export const SELECT = 'detail.select';
export const REQUEST = 'detail.request';
export const RESPONSE = 'detail.response';
export const INVALIDATE = 'detail.invalidate';
export const UPDATE_VALUE = 'detail.update_value';
export const RESET = 'detail.reset';

/**
 * Zneplatnění dat.
 * @param area oblast
 * @param id pokud je poskytnuto ID pokus o invalidaci pouze pokud je ID stejné
 * @returns {{type: string, area: *, data: *}}
 */
export function invalidate(area, id = null) {
    return {
        type: INVALIDATE,
        area,
        id,
    };
}

/**
 * Načtení dat pokud je potřeba.
 * @param area oblast
 * @param id id objektu, které má být ve store
 * @param getData metoda která má vrátit promise pro načtení, vstupem dostane id
 */
export function fetchIfNeeded(area, id, getData, force = false) {
    return (dispatch, getState) => {
        // Spočtení data key - se správným id
        var store = storeFromArea(getState(), area);

        // Pokud ve store není potřebné id, dáme ho tam, aby se nám správně spočetl data key
        if (store.id !== id) {
            dispatch(select(area, id));
            store = storeFromArea(getState(), area);
        }

        const dataKey = store.getDataKey.bind(store)();
        if (force || store.currentDataKey !== dataKey || (!store.isFetching && !store.fetched)) {
            // pokus se data key neschoduje, provedeme fetch
            dispatch(request(area, dataKey));

            if (id !== null) {
                // pokud chceme reálně načíst objekt, provedeme fetch přes getData
                return getData(id).then(json => {
                    const newStore = storeFromArea(getState(), area);
                    const newDataKey = newStore.getDataKey.bind(newStore)();
                    if (newDataKey === dataKey) {
                        // jen pokud příchozí objekt odpovídá dtům, které chceme ve store
                        dispatch(response(area, json));
                    }
                    return Promise.resolve(json);
                }); // TODO [stanekpa] - řešit catch a vrácení datakey!
            } else {
                // Response s prázdným objektem
                dispatch(response(area, null));
                return Promise.resolve(null);
            }
        }
        return Promise.resolve(store.data);
    };
}

/**
 * Test, předaná akce je akcí pro tuto třídu.
 * @param action akce
 * @returns {boolean} true, pokud action odpovídá této třídě
 */
export function is(action) {
    switch (action.type) {
        case REQUEST:
        case RESPONSE:
        case SELECT:
        case INVALIDATE:
        case UPDATE_VALUE:
            return true;
        default:
            return false;
    }
}

/**
 * Úprava hodnoty ve store - vykonána uživatelem/programem
 * @param area oblast
 * @param id dat
 * @param data nová data
 * @returns {{type: string, area: *, data: *}}
 */
export function updateValue(area, id, data) {
    return {
        type: UPDATE_VALUE,
        area,
        id,
        data,
    };
}

/**
 * Výběr nového detailu, volá se i v případě, že je zavoláno fetchIfNeeded s jiným id něž ve store.
 * @param area oblast
 * @param id id
 * @returns {{type: string, area: *, id: *}}
 */
export function select(area, id) {
    return {
        type: SELECT,
        area,
        id,
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

/**
 * Response dat.
 * @param area oblast
 * @param data nová data
 * @returns {{type: string, area: *, data: *}}
 */
function response(area, data) {
    return {
        type: RESPONSE,
        area,
        data,
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
