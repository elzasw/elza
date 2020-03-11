import {WebApi} from 'actions/index.jsx';

import {SimpleListActions} from 'shared/list';
import {DetailActions} from 'shared/detail';
import {indexById, storeFromArea} from 'shared/utils';
import {refExternalSystemListInvalidate} from 'actions/refTables/externalSystems';

export const AREA_EXT_SYSTEM_LIST = 'extSystemList';
export const AREA_EXT_SYSTEM_DETAIL = 'extSystemDetail';

/**
 * Načtení seznamu osob
 *
 */
export function extSystemListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_EXT_SYSTEM_LIST, null, () => WebApi.getAllExtSystem().then(json => {
        return {
            rows: json,
            count: json.count,
        };
    }));
}

export function extSystemDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        dispatch(DetailActions.fetchIfNeeded(AREA_EXT_SYSTEM_DETAIL, id, () => {
            return WebApi.getExtSystem(id).catch(() => dispatch(extSystemDetailClear()));
        }));
    };
}

/**
 * Filtr osob
 *
 * @param filter {Object} - objekt filtru
 */
export function extSystemListFilter(filter) {
    return SimpleListActions.filter(AREA_EXT_SYSTEM_LIST, filter);
}

/**
 * Invalidace seznamu externích systémů
 */
export function extSystemListInvalidate() {
    return SimpleListActions.invalidate(AREA_EXT_SYSTEM_LIST, null);
}

export function extSystemDetailInvalidate() {
    return DetailActions.invalidate(AREA_EXT_SYSTEM_DETAIL, null);
}

export function extSystemDetailClear() {
    return extSystemDetailFetchIfNeeded(null);
}

/**
 * Přidání externího systémů
 */
export function extSystemCreate(data) {
    return (dispatch, getState) => {
        return WebApi.createExtSystem(data).then(response => {
            dispatch(extSystemListInvalidate());
            dispatch(extSystemDetailFetchIfNeeded(response.id));
            dispatch(refExternalSystemListInvalidate());
        });
    };
}

/**
 * Editace externího systémů
 */
export function extSystemUpdate(data) {
    return (dispatch, getState) => {
        const id = data.id;
        return WebApi.updateExtSystem(id, data).then(response => {
            const store = getState();
            const detail = storeFromArea(store, AREA_EXT_SYSTEM_DETAIL);
            const list = storeFromArea(store, AREA_EXT_SYSTEM_LIST);
            if (detail.id === response.id) {
                dispatch(extSystemDetailInvalidate());
            }

            if (list.rows && indexById(list.rows, response.id) !== null) {
                dispatch(extSystemListInvalidate());
            }
            dispatch(refExternalSystemListInvalidate());
        });
    };
}

/**
 * Smazání externího systémů
 */
export function extSystemDelete(id) {
    return (dispatch, getState) => {
        WebApi.deleteExtSystem(id).then(response => {
            const store = getState();
            const detail = storeFromArea(store, AREA_EXT_SYSTEM_DETAIL);
            const list = storeFromArea(store, AREA_EXT_SYSTEM_LIST);
            if (detail.id === id) {
                dispatch(extSystemDetailClear());
            }

            if (list.rows && indexById(list.rows, id) !== null) {
                dispatch(extSystemListInvalidate());
            }
            dispatch(refExternalSystemListInvalidate());
        });
    };
}
