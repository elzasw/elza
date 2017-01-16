import {SimpleListActions} from 'shared/list'

import {WebApi} from 'actions/index.jsx';

export const AREA = 'regExtSystemList';

/**
 * Načtení seznamu reg ext systémů
 */
export function regExtSystemListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA, true, () => WebApi.getRegExternalSystems().then(json => ({rows: json, count: 0})));
}

/**
 * Invalidace seznamu osob
 */
export function regExtSystemListInvalidate() {
    return DetailActions.invalidate(AREA, null);
}
