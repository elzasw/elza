import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'

import {WebApi} from 'actions/index.jsx';

export const AREA = 'regExtSystemList';

/**
 * Načtení seznamu reg ext systémů
 */
export function regExtSystemListFetchIfNeeded() {
    return DetailActions.fetchIfNeeded(AREA, true, () => WebApi.getRegExternalSystems())
}

/**
 * Invalidace seznamu osob
 */
export function regExtSystemListInvalidate() {
    return DetailActions.invalidate(AREA, null);
}
