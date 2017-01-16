import {WebApi} from 'actions/index.jsx';


export const AREA_EXT_SYSTEM_LIST = 'extSystemList';
export const AREA_EXT_SYSTEM_DETAIL = 'extSystemDetail';

/**
 * Načtení seznamu osob dle filtru
 */
export function extSystemListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_EXT_SYSTEM_LIST, true, (parent, filter) => WebApi.getAllExtSystem())
}


/**
 * Invalidace seznamu osob
 */
export function extSystemListInvalidate() {
    return SimpleListActions.invalidate(AREA_EXT_SYSTEM_LIST, null);
}

export function extSystemDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        dispatch(DetailActions.fetchIfNeeded(AREA_EXT_SYSTEM_DETAIL, id, () => {
            return WebApi.getExtSystem(id).catch(()=>dispatch(extSystemDetailClear()));
        }));
    }
}

export function extSystemDetailInvalidate() {
    return DetailActions.invalidate(AREA_EXT_SYSTEM_DETAIL, null)
}

export function extSystemDetailClear() {
    return extSystemDetailFetchIfNeeded(null);
}
