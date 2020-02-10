/**
 * Akce pro skupiny v pořádání.
 */
import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail'

export const REF_GROUPS = 'refTables.groups';

export function invalidate() {
    return DetailActions.invalidate(REF_GROUPS, null);
}

export function fetchIfNeeded(fundVersionId) {
    return DetailActions.fetchIfNeeded(REF_GROUPS, fundVersionId, (id, filter) => WebApi.getGroups(id).then(groups => {
        let result = {
            ids: [],    // mapa kódů skupin, která zaručuje pořadí skupin
            reverse: {} // reverzní mapa typů atributů (identifikátory) na kódy skupin
        };
        for (let group of groups) {
            if (result[group.code]) {
                console.error('Duplicate group code: ' + group.code);
            }
            result[group.code] = group;
            result.ids.push(group.code);

            for (let itemType of group.itemTypes) {
                result.reverse[itemType.id] = group.code;
            }
        }
        return result;
    }));
}
