/**
 * Akce pro typy externích id.
 */
import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail'

export const REF_EID_TYPES = 'refTables.eidTypes';

export function invalidate() {
    return DetailActions.invalidate(REF_EID_TYPES, null);
}

export function fetchIfNeeded() {
    return DetailActions.fetchIfNeeded(REF_EID_TYPES, true, () => WebApi.getEidTypes().then(types => {
        let result = {
            reverse: {} // reverzní mapa typů (kódy na identifikátory)
        };
        for (let type of types) {
            result[type.id] = type;
            result.reverse[type.code] = type.id;
        }
        return result;
    }));
}
