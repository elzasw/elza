import * as types from 'actions/constants/actionTypes';
import {i18n} from 'components'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormValueValidate} from 'actions/arr/subNodeForm'
import {getDescItemType, updateFormData, createDescItem} from './subNodeFormUtils'

function getLoc(state, valueLocation) {
    var descItemGroup = state.formData.descItemGroups[valueLocation.descItemGroupIndex];
    var descItemType = descItemGroup.descItemTypes[valueLocation.descItemTypeIndex];
    var descItem;
    if (typeof valueLocation.descItemIndex !== 'undefined') {
        descItem = descItemType.descItems[valueLocation.descItemIndex];
    }

    return {
        descItemGroup,
        descItemType,
        descItem
    }
}

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    versionId: null,
    nodeId: null,
    data: null,
    getLoc: getLoc
}

function validate(descItem, descItemTypeInfo, valueServerError) {
    var error = {};

    // Specifikace
    if (descItemTypeInfo.useSpecification) {
        if (typeof descItem.descItemSpecId == 'undefined' || descItem.descItemSpecId == "") {
            error.spec = i18n('subNodeForm.validate.spec.required');
        }
    }

    // Hodnota
    switch (descItemTypeInfo.rulDataType.code) {
        case 'PARTY_REF':
        case 'RECORD_REF':
            if (!descItem.value || typeof descItem.value !== 'number') {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case 'PACKET_REF':
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case 'ENUM':
            break;
        case 'FORMATTED_TEXT':
            break;
        case 'UNITDATE':
            if (typeof descItem.calendarTypeId == 'undefined' || descItem.calendarTypeId == "") {
                error.calendarType = i18n('subNodeForm.validate.calendarType.required');
            }
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case 'UNITID':
            break;
        case 'TEXT':
            break;
        case 'STRING':
            break;
        case 'INT':
            if (descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case 'COORDINATES':
            break;
        case 'DECIMAL':
            if (descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        default:
            break;
    }

    // Server validační chyba
    if (valueServerError) {
        if (error.value) {
            error.value += " " + valueServerError;
        } else {
            error.value = valueServerError;
        }
    }

    error.hasError = error.spec || error.value || error.calendarType;

    return error;
}



export default function subNodeForm(state = initialState, action) {
    switch (action.type) {
        case types.FA_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
            var loc = getLoc(state, action.valueLocation);
            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];

            var valueServerError;
            if (!action.result.valid) {
                valueServerError = action.result.message;
            }
            loc.descItem.error = validate(loc.descItem, descItemTypeInfo, valueServerError);

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
            var loc = getLoc(state, action.valueLocation);
            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];
            switch (descItemTypeInfo.rulDataType.code) {
                case 'UNITDATE':
                    loc.descItem.value = action.value.value;
                    loc.descItem.calendarTypeId = action.value.calendarTypeId;

                    // Časovač
                    if (loc.descItem.validateTimer) {
                        clearTimeout(loc.descItem.validateTimer);
                    }
                    var fc = () => action.dispatch(faSubNodeFormValueValidate(action.versionId, action.nodeId, action.nodeKey, action.valueLocation));
                    loc.descItem.validateTimer = setTimeout(fc, 250);
                    break;
                default:
                    loc.descItem.value = action.value;

                    // TODO: vyřešit lépe - probrat s Pavlem
                    if (types.FA_SUB_NODE_FORM_VALUE_CHANGE_PARTY == action.type) {
                        loc.descItem.value = action.value.partyId;
                        loc.descItem.party = action.value;
                    }
                    if (types.FA_SUB_NODE_FORM_VALUE_CHANGE_RECORD == action.type) {
                        loc.descItem.value = action.value.recordId;
                        loc.descItem.record = action.value;
                    }

                    break;
            }
            loc.descItem.touched = true;
            loc.descItem.error = validate(loc.descItem, descItemTypeInfo);

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            var loc = getLoc(state, action.valueLocation);
            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];
            
            loc.descItem.descItemSpecId = action.value;
            loc.descItem.touched = true;
            loc.descItem.error = validate(loc.descItem, descItemTypeInfo);

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_BLUR:
            var loc = getLoc(state, action.valueLocation);

            loc.descItem.hasFocus = false;
            loc.descItemType.hasFocus = false;
            loc.descItemGroup.hasFocus = false;

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_FOCUS:
            var loc = getLoc(state, action.valueLocation);

            loc.descItem.visited = true;
            loc.descItem.hasFocus = true;
            loc.descItemType.hasFocus = true;
            loc.descItemGroup.hasFocus = true;
            
            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_ADD:
            var loc = getLoc(state, action.valueLocation);

            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];

            var descItem = createDescItem(descItemTypeInfo, true);
            descItem.position = loc.descItemType.descItems.length + 1;
            loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];
            
            state.formData = {...state.formData};
            return {...state};
        case types.CHANGE_DESC_ITEM:
            return {...state, dirty: true}
        case types.FA_SUB_NODE_FORM_VALUE_RESPONSE:
            var loc = getLoc(state, action.valueLocation);

            state.data.node = action.descItemResult.node;

            switch (action.operationType) {
                case 'DELETE':
                    // Aktualizace position
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
                    break;
                case 'UPDATE':
                    loc.descItem.descItemObjectId = action.descItemResult.descItem.descItemObjectId;
                    loc.descItem.prevValue = action.descItemResult.descItem.value;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.descItem.descItemSpecId;
                    }
                    break;
                case 'CREATE':
                    loc.descItem.descItemObjectId = action.descItemResult.descItem.descItemObjectId;
                    loc.descItem.id = action.descItemResult.descItem.id;
                    loc.descItem.prevValue = action.descItemResult.descItem.value;
                    loc.descItem.party = action.descItemResult.descItem.party;
                    loc.descItem.record = action.descItemResult.descItem.record;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.descItem.descItemSpecId;
                    }

                    // Aktualizace position - pokud by create byl na první hodnotě a za ní již nějaké uživatel uložil, musí se vše aktualizovat
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});

                    break;
                case 'DELETE_DESC_ITEM_TYPE':
                    // nic dalšího není potřeba, node se aktualizuje výše
                    break;
            }

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
            // Dohledání skupiny a desc item type
            var addGroup, addItemType;
            state.data.descItemTypeGroups.forEach(descItemGroup => {
                descItemGroup.descItemTypes.forEach(descItemType => {
                    if (descItemType.id == action.descItemTypeId) {
                        addGroup = descItemGroup;
                        addItemType = descItemType;
                    }
                });
            });

            // ##
            // # Přidání do formuláře
            // ##

            // Dohledání skupiny, pokud existuje
            var descItemGroup = state.formData.descItemGroups[indexById(state.formData.descItemGroups, addGroup.id)];
            if (!descItemGroup) {   // skupina není, je nutné ji nejdříve přidat a následně seřadit skupiny podle pořadí
                descItemGroup = {...addGroup, descItemTypes: []};
                state.formData.descItemGroups.push(descItemGroup);

                // Seřazení
                state.formData.descItemGroups.sort((a, b) => state.data.descItemTypeGroups[a.id].position - state.data.descItemTypeGroups[a.id].position);
            }

            // Přidání prvku do skupiny a seřazení prvků podle position
            var descItemType = {...addItemType, descItems: []};
            descItemGroup.descItemTypes.push(descItemType);
            // Pokud je ale atribut jednohodnotový, musíme ponechat prázdnou hodnotu
            if (!descItemType.repeatable) {
                var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, addItemType.id)];
                var descItem = createDescItem(descItemTypeInfo, true);
                descItem.position = 1;
                descItemType.descItems.push(descItem);
            }
            descItemGroup.descItemTypes.sort((a, b) => a.viewOrder - b.viewOrder);

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
            var loc = getLoc(state, action.valueLocation);

            // Odebereme pouze pokud je pole jiné než: REQUIRED nebo RECOMMENDED
            if (loc.descItemType.type == 'REQUIRED' || loc.descItemType.type == 'RECOMMENDED') { // ponecháme, pouze odebereme hodnoty
                // Hodnoty odebereme
                loc.descItemType.descItems = [];

                // Pokud je ale atribut jednohodnotový, musíme ponechat prázdnou hodnotu
                if (!loc.descItemType.repeatable) {
                    var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];
                    var descItem = createDescItem(descItemTypeInfo, true);
                    descItem.position = 1;
                    loc.descItemType.descItems.push(descItem);
                }
            } else { // kompletně odebereme
                loc.descItemGroup.descItemTypes = [
                    ...loc.descItemGroup.descItemTypes.slice(0, action.valueLocation.descItemTypeIndex),
                    ...loc.descItemGroup.descItemTypes.slice(action.valueLocation.descItemTypeIndex + 1)
                ]
            }

            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_VALUE_DELETE:
            var loc = getLoc(state, action.valueLocation);

            loc.descItemType.descItems = [
                ...loc.descItemType.descItems.slice(0, action.valueLocation.descItemIndex),
                ...loc.descItemType.descItems.slice(action.valueLocation.descItemIndex + 1)
            ];
            
            state.formData = {...state.formData};
            return {...state};
        case types.FA_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_SUB_NODE_FORM_RECEIVE:
            var result = Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                versionId: action.versionId,
                nodeId: action.nodeId,
            })

            updateFormData(result, action.data, action.rulDataTypes);

            return result;
        default:
            return state
    }
}

