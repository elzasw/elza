import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {createDescItemFromDb, getItemType, updateFormData, createDescItem, consolidateDescItems} from './subNodeFormUtils.jsx'
var subNodeFormUtils = require('./subNodeFormUtils.jsx')
import {validateInt, validateDouble, validateCoordinatePoint} from 'components/validate.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import {valuesEquals} from 'components/Utils.jsx'

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
    fetchingId: null,
    fetched: false,
    dirty: false,
    versionId: null,
    nodeId: null,
    data: null,
    formData: null,
    infoGroups: null,
    infoGroupsMap: null,
    infoTypesMap: null,
    refTypesMap: null,
    getLoc: getLoc
}

function validate(descItem, refType, valueServerError) {
    var error = {};

    // Specifikace
    if (refType.useSpecification) {
        if (typeof descItem.descItemSpecId === 'undefined' || descItem.descItemSpecId === '') {
            error.spec = i18n('subNodeForm.validate.spec.required');
        }
    }

    // Hodnota
    switch (refType.dataType.code) {
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
        case 'JSON_TABLE':
            break;
        case 'ENUM':
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
        case 'FORMATTED_TEXT':
        case 'TEXT':
        case 'STRING':
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case 'INT':
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                error.value = validateInt(descItem.value)
            }
            break;
        case 'COORDINATES':
            error.value = validateCoordinatePoint(descItem.value)
            break;
        case 'DECIMAL':
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                error.value = validateDouble(descItem.value)
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

    error.hasError = (error.spec || error.value || error.calendarType) ? true : false;

    return error;
}

export default function subNodeForm(state = initialState, action = {}) {
    // Načtení umístění, pokud bylo v akci předáno
    var loc
    if (action.valueLocation) {
        loc = getLoc(state, action.valueLocation);
    }

    switch (action.type) {
        case types.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
            var refType = state.refTypesMap[loc.descItemType.id]

            var valueServerError;
            if (!action.result.valid) {
                valueServerError = action.result.message;
            }
            loc.descItem.error = validate(loc.descItem, refType, valueServerError);

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_POSITION:
            console.log("BEFORE", loc.descItemType.descItems)
            var descItems = [...loc.descItemType.descItems];

            // // Odebrání přesouvané
            // descItems = [
            //     ...descItems.slice(0, action.valueLocation.descItemIndex),
            //     ...descItems.slice(action.valueLocation.descItemIndex + 1)
            // ]
            //
            // // Přidání přesouvané na správné místo
            // descItems = [
            //     ...descItems.slice(0, action.index),
            //     loc.descItem,
            //     ...descItems.slice(action.index)
            // ]

            descItems.splice(action.index, 0, descItems.splice(action.valueLocation.descItemIndex, 1)[0]);


            loc.descItemType.descItems = descItems
            console.log("AFTER", loc.descItemType.descItems)

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
            var refType = state.refTypesMap[loc.descItemType.id]
            switch (refType.dataType.code) {
                case 'PARTY_REF':
                    loc.descItem.value = action.value.partyId;
                    loc.descItem.party = action.value;
                    break;
                case 'FILE_REF':
                    loc.descItem.value = action.value.id;
                    loc.descItem.file = action.value;
                    loc.descItem.file['@type'] = '.ArrFileVO';
                    break;
                case 'PACKET_REF':
                    loc.descItem.value = action.value.id;
                    loc.descItem.packet = action.value;
                    break;
                case 'RECORD_REF':
                    loc.descItem.value = action.value.recordId;
                    loc.descItem.record = action.value;
                    break;
                case 'UNITDATE':
                    loc.descItem.value = action.value.value;
                    loc.descItem.calendarTypeId = action.value.calendarTypeId;

                    // Časovač na serverovou validaci
                    if (loc.descItem.validateTimer) {
                        clearTimeout(loc.descItem.validateTimer);
                    }
                    var fc = () => action.dispatch(action.formActions.fundSubNodeFormValueValidate(action.versionId, action.routingKey, action.valueLocation));
                    loc.descItem.validateTimer = setTimeout(fc, 250);
                    break;
                default:
                    loc.descItem.value = action.value;
                    break;
            }

            if (valuesEquals(loc.descItem.value, loc.descItem.prevValue)) {
                loc.descItem.touched = false;
            } else {
                loc.descItem.touched = true;
            }

            loc.descItem.error = validate(loc.descItem, refType);

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            var refType = state.refTypesMap[loc.descItemType.id]
            
            loc.descItem.descItemSpecId = action.value;
            loc.descItem.touched = true;
            loc.descItem.error = validate(loc.descItem, refType);

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_BLUR:
            loc.descItem.hasFocus = false;
            loc.descItemType.hasFocus = false;
            loc.descItemGroup.hasFocus = false;

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:
            loc.descItem.visited = true;
            loc.descItem.hasFocus = true;
            loc.descItemType.hasFocus = true;
            loc.descItemGroup.hasFocus = true;
            
            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_ADD:
            var refType = state.refTypesMap[loc.descItemType.id]

            var descItem = createDescItem(loc.descItemType, refType, true);
            descItem.position = loc.descItemType.descItems.length + 1;
            loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];
            
            state.formData = {...state.formData};
            return {...state};
        case types.CHANGE_NODES:
        case types.OUTPUT_CHANGES_DETAIL:
        case types.CHANGE_OUTPUTS:
            return {...state, dirty: true}
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
            state.data.parent = action.copySiblingResult.parent;

            var currentDescItemMap = {}
            loc.descItemType.descItems.forEach(descItem => {currentDescItemMap[descItem.descItemObjectId] = descItem})
            loc.descItemType.descItems = action.copySiblingResult.type.descItems.map(descItem => {
                var newDescItem = createDescItemFromDb(loc.descItemType, descItem)
                var currDescItem = currentDescItemMap[descItem.descItemObjectId]
                if (currDescItem && currDescItem.hasFocus) {
                    newDescItem.hasFocus = true;
                }
                return newDescItem;
            })

            var refType = state.refTypesMap[loc.descItemType.id]
            var infoType = state.infoTypesMap[loc.descItemType.id]

            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(loc.descItemType, infoType, refType, false)

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
            if (state.data.parent.id !== action.descItemResult.parent.id) {
                return state;
            }
            const newState = {...state};
            newState.data.parent = action.descItemResult.parent;

            switch (action.operationType) {
                case 'DELETE':
                    // Aktualizace position
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
                    break;
                case 'UPDATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item.descItemObjectId;
                    loc.descItem.prevValue = action.descItemResult.item.value;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item.calendarTypeId) {
                        loc.descItem.prevCalendarTypeId = action.descItemResult.item.calendarTypeId;
                    }
                    loc.descItem.touched = false
                    break;
                case 'CREATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item.descItemObjectId;
                    loc.descItem.id = action.descItemResult.item.id;
                    loc.descItem.prevValue = action.descItemResult.item.value;
                    loc.descItem.party = action.descItemResult.item.party;
                    loc.descItem.record = action.descItemResult.item.record;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item.calendarTypeId) {
                        loc.descItem.prevCalendarTypeId = action.descItemResult.item.calendarTypeId;
                    }
                    // Aktualizace position - pokud by create byl na první hodnotě a za ní již nějaké uživatel uložil, musí se vše aktualizovat
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});

                    break;
                case 'DELETE_DESC_ITEM_TYPE':
                    // nic dalšího není potřeba, node se aktualizuje výše
                    break;
            }

            newState.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
            // Dohledání skupiny a desc item type
            var addGroup, addItemType;
            state.infoGroups.forEach(group => {
                group.types.forEach(type => {
                    if (type.id == action.descItemTypeId) {
                        addGroup = group;
                        addItemType = type;
                    }
                });
            });

            // ##
            // # Přidání do formuláře
            // ##

            // Dohledání skupiny, pokud existuje
            var grpIndex = indexById(state.formData.descItemGroups, addGroup.code, 'code');
            var descItemGroup;
            if (grpIndex !== null) {
                descItemGroup = state.formData.descItemGroups[grpIndex];
            } else {   // skupina není, je nutné ji nejdříve přidat a následně seřadit skupiny podle pořadí
                descItemGroup = {code: addGroup.code, name: addGroup.name, descItemTypes: []};
                state.formData.descItemGroups.push(descItemGroup);

                // Seřazení
                state.formData.descItemGroups.sort((a, b) => state.infoGroupsMap[a.code].position - state.infoGroupsMap[b.code].position);
            }

            // Přidání prvku do skupiny a seřazení prvků podle position
            var descItemType = {...addItemType, descItems: []};
            descItemGroup.descItemTypes.push(descItemType);
            // Musíme ponechat prázdnou hodnotu
            var refType = state.refTypesMap[descItemType.id]
            var infoType = state.infoTypesMap[descItemType.id]

            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(descItemType, infoType, refType, true)

            descItemGroup.descItemTypes.sort((a, b) => {
                return state.refTypesMap[a.id].viewOrder - state.refTypesMap[b.id].viewOrder
                //return a.viewOrder - b.viewOrder
            });

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
            if (action.onlyDescItems) { // jen desc items, nic víc
                loc.descItemType.descItems = []
            } else {
                var infoType = state.infoTypesMap[loc.descItemType.id]
                var refType = state.refTypesMap[loc.descItemType.id]

                // Odebereme pouze pokud je pole jiné než: REQUIRED nebo RECOMMENDED
                if (infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED') { // ponecháme, pouze odebereme hodnoty
                    // Hodnoty odebereme
                    loc.descItemType.descItems = [];

                    // Upravení a opravení seznamu hodnot, případně přidání prázdných
                    consolidateDescItems(loc.descItemType, infoType, refType, true)
                } else { // kompletně odebereme
                    loc.descItemGroup.descItemTypes = [
                        ...loc.descItemGroup.descItemTypes.slice(0, action.valueLocation.descItemTypeIndex),
                        ...loc.descItemGroup.descItemTypes.slice(action.valueLocation.descItemTypeIndex + 1)
                    ]
                }
            }

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_DELETE:
            loc.descItemType.descItems = [
                ...loc.descItemType.descItems.slice(0, action.valueLocation.descItemIndex),
                ...loc.descItemType.descItems.slice(action.valueLocation.descItemIndex + 1)
            ];

            var infoType = state.infoTypesMap[loc.descItemType.id]
            var refType = state.refTypesMap[loc.descItemType.id]

            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(loc.descItemType, infoType, refType, true)
            
            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                fetchingId: action.nodeId,
                isFetching: true,
            })
        case types.FUND_SUB_NODE_FORM_RECEIVE:
            // ##
            // # Inicializace dat
            // ##

            // Doplnění descItemTypes o rulDataType a další data
            var dataTypeMap = getMapFromList(action.rulDataTypes.items)
            var descItemTypes = action.refDescItemTypes.items.map(type => {
                return {
                    ...type,
                    dataType: dataTypeMap[type.dataTypeId],
                    descItemSpecsMap: getMapFromList(type.descItemSpecs),
                    columnsDefinitionMap: type.columnsDefinition ? getMapFromList(type.columnsDefinition, "code") : null,
                }
            })

            // Sestavení mapy ref descItemType
            var refTypesMap = getMapFromList(descItemTypes)            

            // ##
            // # Result a merge formuláře.
            // ##
            const result = {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                versionId: action.versionId,
                nodeId: action.nodeId
            };

            updateFormData(result, action.data, refTypesMap);

            return result;
        case types.CHANGE_FUND_RECORD:
            return {
                ...state,
                data: {
                    ...state.data,
                    node: {
                        ...state.data.parent,
                        version: action.version
                    }
                }
            }
        default:
            return state
    }
}

