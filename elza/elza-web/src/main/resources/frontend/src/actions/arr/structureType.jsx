/**
 * Akce pro Struktury.
 */


import {DEFAULT_LIST_SIZE} from '../../constants.tsx'
export const DEFAULT_STRUCTURE_TYPE_MAX_SIZE = DEFAULT_LIST_SIZE;

import {SimpleListActions} from 'shared/list'
import {WebApi} from "../../actions/WebApi";

export const AREA = 'arrStructure';

/**
 * Načtení seznamu dle filtru
 */
export function structureTypeFetchIfNeeded(parent, size = DEFAULT_STRUCTURE_TYPE_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA, parent, (parent, filter) => WebApi.findStructureData(parent.fundVersionId, parent.code, filter.text, filter.assignable, filter.from, size))
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
