/**
 * Akce pro skupiny v pořádání.
 */
import {WebApi} from 'actions/index.jsx';
import {DetailActions} from 'shared/detail';
import { i18n } from 'components';

export const REF_GROUPS = 'refTables.groups';

export function invalidate() {
    return DetailActions.invalidate(REF_GROUPS, null);
}

export function generateDefaultGroup (types, groups){
    const defaultGroup = {
        code: "DEFAULT", 
        name: i18n('subNodeForm.descItemGroup.default'), 
        itemTypes: []
    };

    types.forEach((type) => {
        const group = groups.find((group) => group.itemTypes?.find((itemType) => itemType.id === type.id));
        if(!group || group === defaultGroup.code){
            defaultGroup.itemTypes.push(type)
        }
    })

    return defaultGroup;
}

export function fetchIfNeeded(fundVersionId) {
    return (dispatch, getState) => dispatch(DetailActions.fetchIfNeeded(REF_GROUPS, fundVersionId, (id, filter) => {
        return WebApi.getGroups(id).then(groups => {
            const {descItemTypes} = getState().refTables;

            let result = {
                ids: [], // mapa kódů skupin, která zaručuje pořadí skupin
                reverse: {}, // reverzní mapa typů atributů (identifikátory) na kódy skupin
            };

            // generate default group for descItems without one
            groups.push(generateDefaultGroup(descItemTypes.items, groups));

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
        })
    }
    ));
}
