import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/shared';
import {getMapFromList, indexById} from 'stores/app/utils.jsx'
import {
    consolidateDescItems,
    createDescItem,
    createDescItemFromDb,
    getItemType,
    mergeAfterUpdate,
    updateFormData
} from './subNodeFormUtils.jsx'
import {validateCoordinatePoint, validateDouble, validateInt, validateDuration} from 'components/validate.jsx'
import {valuesEquals} from 'components/Utils.jsx'
import {DisplayType} from "../../../constants.tsx";
import {buildIgnoreMap, endWith, startWith} from "../../../components/Utils";

const FORM_KEY = "formKey"; // klíč verze formuláře
const UID = "_uid"; // virtuální identifikátor hodnoty atributu (jedná se buď o objectId a nebo virtuální klíč v případě, že ještě hodnota atributu nebyla uložena na serveru)
const IS_FETCHING = "isFetching"; // pokud se data načítají
const DIRTY = "dirty"; // zneplatněná data

export const SUB_NODE_FORM_CMP = buildIgnoreMap(endWith(FORM_KEY), endWith(UID));
export const NODE_SUB_NODE_FORM_CMP = buildIgnoreMap(startWith(IS_FETCHING), startWith(DIRTY), endWith(FORM_KEY), endWith(UID));

function getLoc(state, valueLocation) {
    const formData = state.formData;
    if(!formData){
        console.warn("formData do not exist");
        return null;
    }
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
    needClean: false,   // pokud je true, přenačtou se data a vymaže se aktuální editace - obdoba jako nové zobrazení formuláře
    versionId: null,
    nodeId: null,
    data: null,
    formData: null,
    infoGroups: null,
    infoGroupsMap: null,
    infoTypesMap: null,
    refTypesMap: null,
    addItemTypeIds: null,
    getLoc: getLoc
}

export function validate(descItem, refType, valueServerError) {
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
        case 'STRUCTURED':
            if (!descItem.value || typeof descItem.value !== 'number') {
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
                if (refType.viewDefinition === DisplayType.DURATION) {
                    error.value = validateDuration(descItem.value);
                } else {
                    error.value = validateInt(descItem.value);
                }
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
        case 'DATE':
            if (!descItem.value || descItem.value.length === 0) {
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

    error.hasError = (error.spec || error.value || error.calendarType) ? true : false;

    return error;
}

/*
* Converts the value to specified type through the dataTypeMap
*/
export function convertValue(value, descItem, type) {
    //  Data type to value conversion functions map
    const dataTypeMap = {
        PARTY_REF: (value)=>{
            return {
                value: value.id,
                party: value
            };
        },
        FILE_REF: (value)=>{
            return {
                value: value.id,
                file: value
            };
        },
        STRUCTURED: (value)=>{
            return {
                value: value.id,
                structureData: value
            };
        },
        RECORD_REF: (value)=>{
            return {
                value: value.id,
                record: value
            };
        },
        UNITDATE: (value, descItem)=>{
            // change touched attribute when calendarTypeId changed
            const touched = descItem.calendarTypeId !== value.calendarTypeId;
            return {
                value: value.value,
                touched,
                calendarTypeId: value.calendarTypeId
            };
        },
        DEFAULT: (value)=>{
            return {value};
        }
    };
    const convertFunction = dataTypeMap[type];
    if(convertFunction){
        return convertFunction(value, descItem);
    } else {
        return dataTypeMap["DEFAULT"](value);
    }
}

/**
 * Přidání typu atributu do formuláře.
 *
 * @param state      upravovaný state
 * @param itemTypeId přidávaný typ
 */
function addItemType(state, itemTypeId) {
    let addGroup, addItemType;
    state.infoGroups.forEach(group => {
        group.types.forEach(type => {
            if (type.id == itemTypeId) {
                addGroup = group;
                addItemType = type;
            }
        });
    });

    // ##
    // # Přidání do formuláře
    // ##

    // Dohledání skupiny, pokud existuje
    let grpIndex = indexById(state.formData.descItemGroups, addGroup.code, 'code');
    let descItemGroup;
    if (grpIndex !== null) {
        descItemGroup = state.formData.descItemGroups[grpIndex];
    } else {   // skupina není, je nutné ji nejdříve přidat a následně seřadit skupiny podle pořadí
        descItemGroup = {code: addGroup.code, name: addGroup.name, descItemTypes: []};
        state.formData.descItemGroups.push(descItemGroup);

        // Seřazení
        state.formData.descItemGroups.sort((a, b) => state.infoGroupsMap[a.code].position - state.infoGroupsMap[b.code].position);
    }

    // Přidání prvku do skupiny a seřazení prvků podle position
    let descItemType = {...addItemType, descItems: []};
    descItemGroup.descItemTypes.push(descItemType);
    // Musíme ponechat prázdnou hodnotu
    let refType = state.refTypesMap[descItemType.id];
    let infoType = state.infoTypesMap[descItemType.id];

    // Upravení a opravení seznamu hodnot, případně přidání prázdných
    consolidateDescItems(descItemType, infoType, refType, true);

    descItemGroup.descItemTypes.sort((a, b) => {
        return indexById(descItemGroup.types, a.id) - indexById(descItemGroup.types, b.id);
    });

    state.formData = {...state.formData};
}

function addValue(state, loc) {
    let refType = state.refTypesMap[loc.descItemType.id];

    let descItem = createDescItem(loc.descItemType, refType, true);
    descItem.position = loc.descItemType.descItems.length + 1;
    loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];

    state.formData = {...state.formData};
}

export default function subNodeForm(state = initialState, action = {}) {
    // Načtení umístění, pokud bylo v akci předáno
    let loc
    if (action.valueLocation) {
        loc = getLoc(state, action.valueLocation);
    }

    switch (action.type) {
        case types.FUND_FUND_CHANGE_READ_MODE:
            if (action.readMode) {  // změna na read mode - musíme vyresetovat všechny změny ve formuláři
                return {
                    ...state,
                    needClean: true,
                };
            } else {
                return state;
            }
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
            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
            var refType = state.refTypesMap[loc.descItemType.id];
            const convertedValue = convertValue(action.value, loc.descItem, refType.dataType.code);
            // touched if new value is not equal with previous value, or something else changed during conversion
            const touched = convertedValue.touched || !valuesEquals(loc.descItem.value, loc.descItem.prevValue);
            loc.descItem = {
                ...loc.descItem,
                ...convertedValue,
                touched
            };
            // Unitdate server validation
            if(refType.dataType.code === "UNITDATE"){
                    if (loc.descItem.validateTimer) {
                        clearTimeout(loc.descItem.validateTimer);
                    }
                    // FIXME @randak tohle je blbě
                var fc = () => action.dispatch(
                    action.formActions.fundSubNodeFormValueValidate(action.versionId, action.routingKey, action.valueLocation)
                );
                    loc.descItem.validateTimer = setTimeout(fc, 250);
            }
            loc.descItem.error = validate(loc.descItem, refType);

            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            var refType = state.refTypesMap[loc.descItemType.id]

            if (loc.descItem.descItemSpecId !== action.value) {
                loc.descItem.descItemSpecId = action.value;
                loc.descItem.touched = true;
                loc.descItem.error = validate(loc.descItem, refType);

                state.formData = {...state.formData};
                return {...state};
            } else {
                return state;
            }
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
        case types.FUND_SUB_NODE_FORM_VALUE_CREATE:
            loc.descItem.saving = true;
            state.formData = {...state.formData};
            return {...state};
        case types.FUND_SUB_NODE_FORM_VALUE_ADD:
            addValue(state, loc);
            return {...state};
        case types.CHANGE_NODES:
        case types.OUTPUT_CHANGES_DETAIL:
        case types.OUTPUT_CHANGES:
        case types.CHANGE_OUTPUTS:
        case types.CHANGE_STRUCTURE:
        case types.FUND_INVALID:
            return {...state, dirty: true}
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
            state.data.parent = action.copySiblingResult.node;

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
        case types.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH: {
            const infoType = state.infoTypesMap[loc.descItemType.id];
            return {
                ...state,
                infoTypesMap: {
                    ...state.infoTypesMap,
                    [loc.descItemType.id]: {
                        ...infoType,
                        calSt: 1 === infoType.calSt ? 0 : 1
                    }
                }
            }
        }
        case types.FUND_NODE_INCREASE_VERSION:
            if (state.data === null || state.data.parent.id !== action.nodeId || state.data.parent.version !== action.nodeVersionId) { // není pro nás nebo již bylo zavoláno
                return state;
            }

            return {
                ...state,
                data: {
                    ...state.data,
                    parent: {
                        id: action.nodeId,
                        version: action.nodeVersionId + 1,
                    }
                }
            }
        case types.OUTPUT_INCREASE_VERSION:
           if(state.data === null || state.data.parent.id !==action.outputId || state.data.parent.version != action.outputVersion) {
                console.error("Received unexpected increase output version", state, action)
                return state;
           }
           return {
            ...state,
            data: {
                ...state.data,
                parent: {
                    ...state.data.parent,
                    version: action.outputVersion + 1,
                }
            }
        }

        case types.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
            console.log("sub node response", state.data, action)
            let node = action.descItemResult.node || action.descItemResult.parent;
            if (state.data.parent.id !== node.id) {
                return state;
            }
            const newState = {...state};
            newState.data.parent = node;

            switch (action.operationType) {
                case 'DELETE':
                    // Aktualizace position
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
                    break;
                case 'UPDATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item ? action.descItemResult.item.descItemObjectId : null;
                    loc.descItem.prevValue = action.descItemResult.item ? action.descItemResult.item.value : null;
                    if (action.descItemResult.item && loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item && action.descItemResult.item.calendarTypeId) {
                        loc.descItem.prevCalendarTypeId = action.descItemResult.item.calendarTypeId;
                    }
                    loc.descItem.touched = false;
                    break;
                case 'CREATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item.descItemObjectId;
                    loc.descItem.id = action.descItemResult.item.id;
                    loc.descItem.prevValue = action.descItemResult.item.value;
                    loc.descItem.party = action.descItemResult.item.party;
                    loc.descItem.record = action.descItemResult.item.record;
                    loc.descItem.saving = false;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item.calendarTypeId) {
                        loc.descItem.prevCalendarTypeId = action.descItemResult.item.calendarTypeId;
                    }
                    loc.descItem.touched = false;
                    // Aktualizace position - pokud by create byl na první hodnotě a za ní již nějaké uživatel uložil, musí se vše aktualizovat
                    loc.descItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
                    break;
                case 'DELETE_DESC_ITEM_TYPE':
                    // nic dalšího není potřeba, node se aktualizuje výše
                    break;
            }

            newState.formData = {...state.formData};
            return {...state};

        case types.FUND_SUB_NODE_FORM_TEMPLATE_USE: {
            const groups = action.groups;
            const template = action.template;
            const addItemTypeIds = action.addItemTypeIds;
            const formData = template.formData;
            const replaceValues = template.replaceValues;

            Object.keys(formData).map(itemTypeId => {
                let existsItemType = false;
                const items = formData[itemTypeId];

                const groupCode = groups.reverse[itemTypeId];
                const group = groups[groupCode];

                const addItemType = state.infoTypesMap[itemTypeId];

                // Dohledání skupiny, pokud existuje
                const grpIndex = indexById(state.formData.descItemGroups, groupCode, 'code');

                let itemsMerge = [];
                let descItemGroup;
                if (grpIndex !== null) {
                    descItemGroup = state.formData.descItemGroups[grpIndex];

                    const index = indexById(descItemGroup.descItemTypes, itemTypeId);

                    if (index !== null) {
                        existsItemType = true;
                        const itemType = descItemGroup.descItemTypes[index];
                        itemsMerge = itemType.descItems;
                        if (itemType.rep) {
                            items.forEach((item => {
                                const {value, ...newItem} = item; // odebrání hodnoty
                                itemsMerge.push(newItem);
                                if (!value) {
                                    if (!state.addItemTypeIds) {
                                        state.addItemTypeIds = [];
                                    }
                                    state.addItemTypeIds.push(itemType.id);
                                }
                            }));
                        }
                    }

                } else {   // skupina není, je nutné ji nejdříve přidat a následně seřadit skupiny podle pořadí
                    descItemGroup = {code: group.code, name: group.name, descItemTypes: []};

                    items.forEach((item => {
                        const {value, ...newItem} = item; // odebrání hodnoty
                        itemsMerge.push(newItem);
                    }));

                    state.formData.descItemGroups.push(descItemGroup);

                    // Seřazení
                    state.formData.descItemGroups.sort((a, b) => state.infoGroupsMap[a.code].position - state.infoGroupsMap[b.code].position);
                }

                // Přidání prvku do skupiny a seřazení prvků podle position
                const itemType = {...addItemType, descItems: itemsMerge};
                if (!existsItemType) {
                    if (addItemTypeIds) {
                        if (!state.addItemTypeIds) {
                            state.addItemTypeIds = [];
                        }
                        items.forEach((item) => { // musím přidat tolikrát, kolikrát je to v šabloně
                            const {value, ...newItem} = item;
                            if (!value) {
                                state.addItemTypeIds.push(itemType.id);
                            }
                        });
                    }
                    descItemGroup.descItemTypes.push(itemType);
                }
                // Musíme ponechat prázdnou hodnotu
                const refType = state.refTypesMap[itemType.id];
                const infoType = state.infoTypesMap[itemType.id];

                // Upravení a opravení seznamu hodnot, případně přidání prázdných
                consolidateDescItems(itemType, infoType, refType, true);

                descItemGroup.descItemTypes.sort((a, b) => {
                    return state.refTypesMap[a.id].viewOrder - state.refTypesMap[b.id].viewOrder
                    //return a.viewOrder - b.viewOrder
                });

            });

            state.formData = {...state.formData};
            return {...state};
        }

        // Přidá identifikátory typů atributů, které budou s dalším načtením obsahu JP přidány (prázdné)
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPES_ADD_TEMPLATE: {
            state.addItemTypeIds = action.itemTypeIds;
            return {...state};
        }

        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
            addItemType(state, action.descItemTypeId);
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
                    //viewDefinitionMap: type.viewDefinition ? getMapFromList(type.viewDefinition, "code") : null,
                }
            })

            // Sestavení mapy ref descItemType
            var refTypesMap = getMapFromList(descItemTypes)

            // ##
            // # Result a merge formuláře.
            // ##
            var result = {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                versionId: action.versionId,
                nodeId: action.nodeId,
                needClean: false,
            };

            // Pokud je vyžadován reset formuláře, nastavíme předchozí data na null a tím se vše znovu nainicializuje
            if (action.needClean) {
                result.data = null;
                result.formData = null;
            }
            updateFormData(result, action.data, refTypesMap, action.groups, null, state.dirty);

            // Pokud existují typy atributů, které chceme po načtení přidat, přidáme je
            const itemTypeIds = result.addItemTypeIds;
            if (itemTypeIds) {
                const added = {};
                itemTypeIds.forEach(itemTypeId => {
                    if (added[itemTypeId]) {
                        var descItemGroup = null;
                        var descItemType = null;
                        for (let i = 0; i < result.formData.descItemGroups.length; i++) {
                            descItemGroup = result.formData.descItemGroups[i];
                            const itemTypeIndex = indexById(descItemGroup.descItemTypes, itemTypeId);
                            if (itemTypeIndex != null) {
                                descItemType = descItemGroup.descItemTypes[itemTypeIndex];
                                addValue(result, {descItemGroup, descItemType});
                                break;
                            }
                        }
                    } else {
                        addItemType(result, itemTypeId);
                        added[itemTypeId] = true;
                    }
                });
                result.addItemTypeIds = null;
            }

            return result;
        case types.FUND_SUBNODE_UPDATE:
            var {node, parent} = action.data;
            let nodeId = (node && node.id) || (parent && parent.id);

            if (nodeId != state.nodeId){
                // not the right node
                return state;
            }

            var result = {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                versionId: action.versionId,
                nodeId: nodeId,
                needClean: false,
            };

            mergeAfterUpdate(result, action.data, action.refTables); // merges result with data from action

            return result;

        case types.CHANGE_FUND_RECORD:
            return {
                ...state,
                data: {
                    ...state.data,
                    node: {
                        ...state.data.node,
                        version: action.version
                    }
                }
            }
        default:
            return state
    }
}

