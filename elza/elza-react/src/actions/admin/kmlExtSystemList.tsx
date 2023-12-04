import { SimpleListActions } from 'shared/list';

import { WebApi } from 'actions/index.jsx';

export const AREA = 'kmlExtSystemList';

/**
 * Načtení seznamu ap ext systémů
 */
export function kmlExtSystemListFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA, true, () =>
        WebApi.getKmlExternalSystems().then(json => ({ rows: json, count: 0 })),
    );
}
