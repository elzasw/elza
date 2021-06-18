/**
 * Akce pro typy výstupních filtrů.
 */
import {DetailActions} from 'shared/detail';
import {WebApi} from '../WebApi';

export const AREA = 'refTables.outputFilters';

export function invalidate() {
    return DetailActions.invalidate(AREA, null);
}

export function fetchIfNeeded() {
    return DetailActions.fetchIfNeeded(AREA, true, WebApi.findOutputFilters);
}
