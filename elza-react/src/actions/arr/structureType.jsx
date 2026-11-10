/**
 * Akce pro Struktury.
 */

import {DEFAULT_LIST_SIZE} from '../../constants.tsx';
import {SimpleListActions} from 'shared/list';
import storeFromArea from 'shared/utils/storeFromArea';

import {WebApi} from '../../actions/WebApi';

export const DEFAULT_STRUCTURE_TYPE_MAX_SIZE = DEFAULT_LIST_SIZE;

export const AREA = 'arrStructure';

/**
 * Načtení seznamu dle filtru
 */
export function structureTypeFetchIfNeeded(parent, size = DEFAULT_STRUCTURE_TYPE_MAX_SIZE) {
    return (dispatch, getState) => {
        const store = storeFromArea(getState(), AREA);
        // One store serves every structure type and fund, so a filter must not carry over to another one.
        const isParentChanged =
            store.parent !== null &&
            (store.parent.code !== parent.code || store.parent.fundVersionId !== parent.fundVersionId);
        if (isParentChanged) {
            dispatch(structureTypeFilter({...store.filter, text: '', assignable: '', from: 0}));
        }

        dispatch(
            SimpleListActions.fetchIfNeeded(AREA, parent, (parent, filter) =>
                WebApi.findStructureData(parent.fundVersionId, parent.code, filter.text, filter.assignable, filter.from, size),
            ),
        );
    };
}

/**
 * Filtr
 *
 * @param filter {Object} - objekt filtru
 */
export function structureTypeFilter(filter) {
    return SimpleListActions.filter(AREA, filter);
}

/**
 * Invalidace seznamu
 */
export function structureTypeInvalidate() {
    return SimpleListActions.invalidate(AREA, null);
}
