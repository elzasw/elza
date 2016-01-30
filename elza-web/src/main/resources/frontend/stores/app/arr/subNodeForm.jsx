import * as types from 'actions/constants/actionTypes';
import {i18n} from 'components'
import {indexById} from 'stores/app/utils.jsx'
import {faSubNodeFormValueValidate} from 'actions/arr/subNodeForm'

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
    versionId: null,
    nodeId: null,
    data: null,
    getLoc: getLoc
}


function updateFormData(state, rulDataTypes) {
    // Mapa id descItemType na descItemType
    var descItemTypesMap = {};
    state.data.descItemGroups.forEach(group => {
        group.descItemTypes.forEach(descItemType => {
            descItemTypesMap[descItemType.id] = descItemType;
        })
    })

    // Seznam všech atributů - obecně, doplněný o rulDataType
    // Doplnění position ke skupině
    var descItemTypeInfos = [];
    state.descItemTypeGroupsMap = {};
    state.data.descItemTypeGroups.forEach((descItemGroup, descItemGroupIndex) => {
        state.descItemTypeGroupsMap[descItemGroup.id] = descItemGroup;
        descItemGroup.position = descItemGroupIndex;
        descItemGroup.descItemTypes.forEach(descItemType => {
            var rulDataType = rulDataTypes.items[indexById(rulDataTypes.items, descItemType.dataTypeId)];

            var descItemTypeInfo = Object.assign({}, descItemType, { descItemGroup: descItemGroup, rulDataType: rulDataType});
            descItemTypeInfos.push(descItemTypeInfo);
        });
    });

    // Vytvoření formuláře se všemi povinnými a doporučenými položkami, které jsou doplněné reálnými daty ze serveru
    var descItemGroups = [];
    state.data.descItemTypeGroups.forEach(group => {
        var resultGroup = {
            ...group,
            hasFocus: false
        };

        resultGroup.descItemTypes = [];
        group.descItemTypes.forEach(descItemType => {
            var resultDescItemType = {
                ...descItemType,
                hasFocus: false
            }

            var dbDescItemType = descItemTypesMap[descItemType.id];
            var useDescItemType = false;    // jestli se má nakonec objevit na formuláři
            if (dbDescItemType) {   // použijeme DB hodnotu
                useDescItemType = true;

                resultDescItemType.descItems = dbDescItemType.descItems.map(descItem => {
                    return Object.assign(
                        {},
                        descItem,
                        {
                            prevDescItemSpecId: descItem.descItemSpecId,
                            prevValue: descItem.value,
                            hasFocus: false,
                            touched: false,
                            visited: false,
                            error: {hasError:false}
                        }
                    )
                })
            } else {    // není v DB, vytvoříme jen pro možnou inplace editaci
                resultDescItemType.descItems = [];
                if (descItemType.type == 'REQUIRED' || descItemType.type == 'RECOMMENDED') {
                    var rulDataType = rulDataTypes.items[indexById(rulDataTypes.items, descItemType.dataTypeId)];
                    if (!descItemType.repeatable) { // řešíme jen neopakovatelné, u nich to má smysl
                        useDescItemType = true;

                        var descItemTypeInfo = descItemTypeInfos[indexById(descItemTypeInfos, descItemType.id)];                        
                        var descItem = createDescItem(descItemTypeInfo);
                        descItem.position = 1;
                        resultDescItemType.descItems.push(descItem);
                    }
                }
            }

            if (useDescItemType) {
                resultGroup.descItemTypes.push(resultDescItemType);
            }
        });

        if (resultGroup.descItemTypes.length > 0) { // skupinu budeme uvádět pouze pokud má nějaké atributy k zobrazení (povinné nebo doporučené)
            descItemGroups.push(resultGroup);
        }
    })

    var formData = {
        descItemGroups: descItemGroups
    }

    state.formData = formData;
    state.descItemTypeInfos = descItemTypeInfos;
}

function getDescItemType(descItemTypeInfo) {
    switch (descItemTypeInfo.rulDataType.code) {
        case 'TEXT':
            return '.ArrDescItemTextVO';
        case 'STRING':
            return '.ArrDescItemStringVO';
        case 'INT':
            return '.ArrDescItemIntVO';
        case 'COORDINATES':
            return '.ArrDescItemCoordinatesVO';
        case 'DECIMAL':
            return '.ArrDescItemDecimalVO';
        case 'PARTY_REF':
            return '.ArrDescItemPartyRefVO';
        case 'RECORD_REF':
            return '.ArrDescItemRecordRefVO';
        case 'PACKET_REF':
            return '.ArrDescItemPacketVO';
        case 'ENUM':
            return '.ArrDescItemEnumVO';
        case 'FORMATTED_TEXT':
            return '.ArrDescItemFormattedTextVO';
        case 'UNITDATE':
            return '.ArrDescItemUnitdateVO';
        case 'UNITID':
            return '.ArrDescItemUnitidVO';
        default:
            console.error("Unsupported data type", descItemTypeInfo.rulDataType);
            return null;
    }
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
            break;
        case 'RECORD_REF':
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

function createDescItem(descItemTypeInfo) {
    var result = {
        '@type': getDescItemType(descItemTypeInfo),
        prevValue: null,
        hasFocus: false,
        touched: false,
        visited: false,
        value: '',
        error: {hasError:false}
    };

    if (descItemTypeInfo.useSpecification) {
        result.descItemSpecId = '';
    }

    return result;
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

            var descItem = createDescItem(descItemTypeInfo);
            descItem.position = loc.descItemType.descItems.length + 1;
            loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];
            
            state.formData = {...state.formData};
            return {...state};
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
                var descItem = createDescItem(descItemTypeInfo);
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
                    var descItem = createDescItem(descItemTypeInfo);
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
            return state;
        case types.FA_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_SUB_NODE_FORM_RECEIVE:
            var result = Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                versionId: action.versionId,
                nodeId: action.nodeId,
                data: action.data,
            })

            updateFormData(result, action.rulDataTypes);

            return result;
        default:
            return state
    }
}

