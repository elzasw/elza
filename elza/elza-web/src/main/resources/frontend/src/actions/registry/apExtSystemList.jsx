import {SimpleListActions} from 'shared/list'

import {WebApi} from 'actions/index.jsx';

export const AREA = 'apExtSystemList';

/**
 * Načtení seznamu ap ext systémů
 */
export function apExtSystemListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA, true, () => WebApi.getApExternalSystems().then(json => ({rows: json, count: 0})));
}
