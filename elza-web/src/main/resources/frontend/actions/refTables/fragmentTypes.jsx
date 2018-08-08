/**
 * Akce pro typy fragmentů
 */
import {WebApi} from '../../actions/index.jsx';
import {DetailActions} from '../../shared/detail'

export const AREA = 'refTables.fragmentTypes';

export function invalidate() {
    return DetailActions.invalidate(AREA, null);
}

export function fetchIfNeeded() {
    return DetailActions.fetchIfNeeded(AREA, true, () => WebApi.findFragmentTypes());
}
