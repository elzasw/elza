import * as types from 'actions/constants/ActionTypes';
import { i18n } from 'components/shared';
import { getMapFromList, indexById } from 'stores/app/utils';
import { validateCoordinatePoint, validateDouble, validateDuration, validateInt } from 'components/validate';
import { valuesEquals } from 'components/Utils';
import { DisplayType } from '../../../constants';
import { buildIgnoreMap, endWith, startWith } from '../../../components/Utils';
import { cloneDeep } from 'lodash-es';
import {
    prepareNextFormKey,
    isType,
    consolidateDescItems,
    createDescItem,
    createDescItemFromDb,
    mergeAfterUpdate,
    updateFormData,
    checkFormData,
} from './subNodeFormUtils';
import { validateUnitDate } from '../../../components/registry/field/UnitdateField';
import { RulItemTypeType } from '../../../api/RulItemTypeType';

const FORM_KEY = 'formKey'; // klíč verze formuláře
const UID = '_uid'; // virtuální identifikátor hodnoty atributu (jedná se buď o objectId a nebo virtuální klíč v případě, že ještě hodnota atributu nebyla uložena na serveru)
const IS_FETCHING = 'isFetching'; // pokud se data načítají
const DIRTY = 'dirty'; // zneplatněná data

export const SUB_NODE_FORM_CMP = buildIgnoreMap(endWith(FORM_KEY), endWith(UID));
export const NODE_SUB_NODE_FORM_CMP = buildIgnoreMap(
    startWith(IS_FETCHING),
    startWith(DIRTY),
    endWith(FORM_KEY),
    endWith(UID),
);

function setLoc(state, valueLocation, loc, descItemType = true, descItem = true) {
    const formData = state.formData;
    if (!formData) {
        console.warn('formData do not exist');
    }

    const newState = {
        ...state,
        formData: {
            ...state.formData,
        },
    };

    newState.formData.descItemGroups = [...newState.formData.descItemGroups];
    newState.formData.descItemGroups[valueLocation.descItemGroupIndex] = loc.descItemGroup;

    if (descItemType) {
        loc.descItemGroup.descItemTypes = [...loc.descItemGroup.descItemTypes];
        loc.descItemGroup.descItemTypes[valueLocation.descItemTypeIndex] = loc.descItemType;

        if (descItem) {
            if (typeof valueLocation.descItemIndex !== 'undefined') {
                loc.descItemType.descItems = [...loc.descItemType.descItems];
                loc.descItemType.descItems[valueLocation.descItemIndex] = loc.descItem;
            }
        }
    }

    return newState;
}

function getLoc(state, valueLocation) {
    const formData = state.formData;
    if (!formData) {
        console.warn('formData do not exist');
        return null;
    }
    const descItemGroup = { ...state.formData.descItemGroups[valueLocation.descItemGroupIndex] };
    const descItemType = { ...descItemGroup.descItemTypes[valueLocation.descItemTypeIndex] };
    let descItem;
    if (typeof valueLocation.descItemIndex !== 'undefined') {
        descItem = { ...descItemType.descItems[valueLocation.descItemIndex] };
    }

    descItemGroup.descItemTypes = [...descItemGroup.descItemTypes];
    descItemGroup.descItemTypes[valueLocation.descItemTypeIndex] = descItemType;

    descItemType.descItems = descItemType.descItems ? [...descItemType.descItems] : [];
    if (descItem) {
        descItemType.descItems[valueLocation.descItemIndex] = descItem;
    }

    // descItem && console.log("$$$$$$$$$", descItem.descItemSpecId)

    return {
        descItemGroup,
        descItemType,
        descItem,
    };
}

const initialState = {
    isFetching: false,
    fetchingId: null,
    fetched: false,
    dirty: false,
    needClean: false, // pokud je true, přenačtou se data a vymaže se aktuální editace - obdoba jako nové zobrazení formuláře
    versionId: null,
    nodeId: null,
    data: null,
    formData: null,
    infoGroups: null,
    infoGroupsMap: null,
    infoTypesMap: null,
    refTypesMap: null,
    addItemTypeIds: null,
    getLoc: getLoc,
};

export function validate(descItem, refType, valueServerError) {
    const error = {};

    // Specifikace
    if (refType.useSpecification) {
        if (typeof descItem.descItemSpecId === 'undefined' || descItem.descItemSpecId === '') {
            error.spec = i18n('subNodeForm.validate.spec.required');
        }
    }

    // Hodnota
    switch (refType.dataType.code) {
        case 'URI_REF':
            if (!descItem.value) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
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
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                const validateResult = validateUnitDate(descItem.value);
                if (!validateResult.valid) {
                    error.value = validateResult.message;
                }
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
        case 'BIT':
            if (descItem.value === undefined || descItem.value === null) {
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
            error.value = validateCoordinatePoint(descItem.value);
            break;
        case 'DECIMAL':
            if (!descItem.value || descItem.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                error.value = validateDouble(descItem.value);
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
            error.value += ' ' + valueServerError;
        } else {
            error.value = valueServerError;
        }
    }

    error.hasError = error.spec || error.value ? true : false;

    return error;
}

/*
 * Converts the value to specified type through the dataTypeMap
 */
export function convertValue(value, descItem, type) {
    //  Data type to value conversion functions map
    const dataTypeMap = {
        URI_REF: (value, descItem) => {
            return {
                touched:
                    descItem.value !== value.value ||
                    descItem.description !== value.description ||
                    descItem.refTemplateId !== value.refTemplateId,
                value: value.value,
                description: value.description,
                refTemplateId: value.refTemplateId,
            };
        },
        FILE_REF: value => {
            if (value.value) {
                return {
                    value: value.value,
                    file: value.file,
                };
            }
            return {
                value: value.id,
                file: value,
            };
        },
        STRUCTURED: value => {
            if (value.structureData) {
                return {
                    value: value.value,
                    structureData: value.structureData,
                };
            }
            return {
                value: value.id,
                structureData: value,
            };
        },
        RECORD_REF: value => {
            if (value.value) {
                return {
                    value: value.value,
                    record: value.record,
                };
            }
            return {
                value: value.id,
                record: value,
            };
        },
        UNITDATE: (value, descItem) => {
            return {
                value: value.value,
                touched: false,
            };
        },
        DEFAULT: value => {
            return { value };
        },
    };
    const convertFunction = dataTypeMap[type];
    if (convertFunction) {
        return convertFunction(value, descItem);
    } else {
        return dataTypeMap['DEFAULT'](value);
    }
}

/**
 * Vyhledani skupiny a typu atributu v existujicich skupinach podle itemTypeId.
 */
const findGroupAndType = (groups, itemTypeId) => {
    let descItemGroup = null;
    let descItemType = null;
    groups.find((group) => {
        descItemType = group.descItemTypes.find((descItemType) => descItemType.id == itemTypeId)
        if (descItemType != undefined) {
            descItemGroup = group;
            return true;
        }
    })
    return [descItemGroup, descItemType];
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
    } else {
        // skupina není, je nutné ji nejdříve přidat a následně seřadit skupiny podle pořadí
        descItemGroup = { code: addGroup.code, name: addGroup.name, descItemTypes: [] };
        state.formData.descItemGroups.push(descItemGroup);

        // Seřazení
        state.formData.descItemGroups.sort(
            (a, b) => state.infoGroupsMap[a.code].position - state.infoGroupsMap[b.code].position,
        );
    }

    // Přidání prvku do skupiny a seřazení prvků podle position
    let descItemType = { ...addItemType, descItems: [] };
    descItemGroup.descItemTypes.push(descItemType);
    // Musíme ponechat prázdnou hodnotu
    let refType = state.refTypesMap[descItemType.id];
    let infoType = state.infoTypesMap[descItemType.id];

    // Upravení a opravení seznamu hodnot, případně přidání prázdných
    consolidateDescItems(descItemType, infoType, refType, true);

    descItemGroup.descItemTypes.sort((a, b) => {
        return indexById(descItemGroup.types, a.id) - indexById(descItemGroup.types, b.id);
    });

    state.formData = { ...state.formData };
}

function addValue(state, loc) {
    prepareNextFormKey(loc.descItemType);
    let refType = state.refTypesMap[loc.descItemType.id];

    let descItem = createDescItem(loc.descItemType, refType, true);
    descItem.position = loc.descItemType.descItems.length + 1;
    loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];

    state.formData = { ...state.formData };
}

export default function subNodeForm(state = initialState, action = {}) {
    // Načtení umístění, pokud bylo v akci předáno
    let loc;
    if (action.valueLocation) {
        loc = getLoc(state, action.valueLocation);
    }

    switch (action.type) {
        case types.FUND_FUND_CHANGE_READ_MODE:
            if (action.readMode) {
                // změna na read mode - musíme vyresetovat všechny změny ve formuláři
                return {
                    ...state,
                    needClean: true,
                };
            } else {
                return state;
            }
        case types.FUND_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
            var refType = state.refTypesMap[loc.descItemType.id];

            var valueServerError;
            if (!action.result.valid) {
                valueServerError = action.result.message;
            }
            loc.descItem.error = validate(loc.descItem, refType, valueServerError);

            return setLoc(state, action.valueLocation, loc);
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

            // přepočítání pozice
            for (let i = 0; i < descItems.length; i++) {
                descItems[i].position = i + 1;
            }

            loc.descItemType.descItems = descItems;

            // je třeba upravit index (původní index určuje původní pozici descItemu, nový indexu určuje novou pozici)
            return setLoc(
                state,
                {
                    ...action.valueLocation,
                    descItemIndex: action.index,
                },
                loc,
            );
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
            const { valueLocation } = action;
            var refType = state.refTypesMap[loc.descItemType.id];
            const convertedValue = convertValue(action.value, loc.descItem, refType.dataType.code);
            // touched if new value is not equal with previous value, or something else changed during conversion

            const touched = convertedValue.touched || !valuesEquals(convertedValue.value, loc.descItem.prevValue);
            loc.descItem = {
                ...loc.descItem,
                ...convertedValue,
                touched,
            };

            // Validace dat položky
            loc.descItem.error = validate(loc.descItem, refType);

            return setLoc(state, action.valueLocation, loc);
        case types.FUND_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            var refType = state.refTypesMap[loc.descItemType.id];

            if (loc.descItem.descItemSpecId !== action.value) {
                // console.log(777777777777)
                loc.descItem.descItemSpecId = action.value;
                loc.descItem.touched = true;
                loc.descItem.error = validate(loc.descItem, refType);

                return setLoc(state, action.valueLocation, loc);
            } else {
                return state;
            }
        case types.FUND_SUB_NODE_FORM_VALUE_BLUR:
            loc.descItem.hasFocus = false;
            loc.descItemType.hasFocus = false;
            loc.descItemGroup.hasFocus = false;

            return setLoc(state, action.valueLocation, loc);
        case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:
            loc.descItem.visited = true;
            loc.descItem.hasFocus = true;
            loc.descItemType.hasFocus = true;
            loc.descItemGroup.hasFocus = true;

            return setLoc(state, action.valueLocation, loc);
        case types.FUND_SUB_NODE_FORM_VALUE_CREATE:
            loc.descItem.saving = true;

            return setLoc(state, action.valueLocation, loc);
        case types.FUND_SUB_NODE_FORM_VALUE_ADD:
            addValue(state, loc);

            return setLoc(state, action.valueLocation, loc, true, false);
        case types.CHANGE_NODES:
        case types.OUTPUT_CHANGES_DETAIL:
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_STATE_CHANGE:
        case types.CHANGE_OUTPUTS:
        case types.CHANGE_STRUCTURE:
        case types.FUND_INVALID:
            checkFormData(state.formData);
            return { ...state, dirty: true };
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
            state.data.parent = action.copySiblingResult.node;

            var currentDescItemMap = {};
            loc.descItemType.descItems.forEach(descItem => {
                currentDescItemMap[descItem.descItemObjectId] = descItem;
            });
            loc.descItemType.descItems = action.copySiblingResult.type.descItems.map(descItem => {
                var newDescItem = createDescItemFromDb(loc.descItemType, descItem);
                var currDescItem = currentDescItemMap[descItem.descItemObjectId];
                if (currDescItem && currDescItem.hasFocus) {
                    newDescItem.hasFocus = true;
                }
                checkFormData(state.formData);
                return newDescItem;
            });

            var refType = state.refTypesMap[loc.descItemType.id];
            var infoType = state.infoTypesMap[loc.descItemType.id];

            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(loc.descItemType, infoType, refType, false);

            return setLoc(state, action.valueLocation, loc);
        case types.FUND_SUB_NODE_FORM_OUTPUT_CALC_SWITCH: {
            const infoType = state.infoTypesMap[loc.descItemType.id];
            return {
                ...state,
                infoTypesMap: {
                    ...state.infoTypesMap,
                    [loc.descItemType.id]: {
                        ...infoType,
                        calSt: 1 === infoType.calSt ? 0 : 1,
                    },
                },
            };
        }
        case types.FUND_NODE_INCREASE_VERSION:
            if (
                state.data === null ||
                state.data.parent.id !== action.nodeId ||
                state.data.parent.version !== action.nodeVersionId
            ) {
                // není pro nás nebo již bylo zavoláno
                checkFormData(state.formData);
                return state;
            }

            checkFormData(state.formData);
            return {
                ...state,
                data: {
                    ...state.data,
                    parent: {
                        id: action.nodeId,
                        version: action.nodeVersionId + 1,
                    },
                },
            };
        case types.OUTPUT_INCREASE_VERSION:
            if (
                state.data === null ||
                state.data.parent.id !== action.outputId ||
                state.data.parent.version != action.outputVersion
            ) {
                console.error('Received unexpected increase output version', state, action);
                checkFormData(state.formData);
                return state;
            }
            checkFormData(state.formData);
            return {
                ...state,
                data: {
                    ...state.data,
                    parent: {
                        ...state.data.parent,
                        version: action.outputVersion + 1,
                    },
                },
            };

        case types.FUND_SUB_NODE_FORM_VALUE_RESPONSE:
            console.log('sub node response', state.data, action);
            let node = action.descItemResult.node || action.descItemResult.parent;
            if (!state.data || !state.data.parent) {
                if (state.isFetching) {
                    // probíhá nahrávání formuláře - změna stavu poslední hodnoty může být ignorována
                    // čekáme na donahrání dat
                    return state;
                }
                // toto je zvláštní situace a nerozumíme, kdy k ní dochází
                // data by mela vzdy existovat pri prijmu odpovedi
                console.error('unexpected state - missing data', state, action);
                throw new Error(
                    `Neočekávaný stav při akci: FUND_SUB_NODE_FORM_VALUE_RESPONSE\nstate: ` + JSON.stringify(state),
                );
            }
            if (state.data.parent.id !== node.id) {
                checkFormData(state.formData);
                return state;
            }
            let newState = cloneDeep(state);
            loc = getLoc(newState, action.valueLocation);
            newState.data.parent = node;

            switch (action.operationType) {
                case 'DELETE':
                    // Aktualizace position
                    loc.descItemType.descItems.forEach((descItem, index) => {
                        descItem.position = index + 1;
                    });
                    newState = setLoc(newState, action.valueLocation, loc, true, false);
                    break;
                case 'UPDATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item
                        ? action.descItemResult.item.descItemObjectId
                        : null;
                    loc.descItem.prevValue = action.descItemResult.item ? action.descItemResult.item.value : null;
                    if (action.descItemResult.item && loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item && action.descItemResult.item.description) {
                        loc.descItem.prevDescription = action.descItemResult.item.description;
                    }
                    if (action.descItemResult.item && action.descItemResult.item.refTemplateId) {
                        loc.descItem.prevRefTemplateId = action.descItemResult.item.refTemplateId;
                    }
                    loc.descItem.touched = false;
                    newState = setLoc(newState, action.valueLocation, loc, true, true);
                    break;
                case 'CREATE':
                    loc.descItem.descItemObjectId = action.descItemResult.item.descItemObjectId;
                    loc.descItem.id = action.descItemResult.item.id;
                    loc.descItem.prevValue = action.descItemResult.item.value;
                    loc.descItem.record = action.descItemResult.item.record;
                    loc.descItem.saving = false;
                    if (loc.descItemType.useSpecification) {
                        loc.descItem.prevDescItemSpecId = action.descItemResult.item.descItemSpecId;
                    }
                    if (action.descItemResult.item.description) {
                        loc.descItem.prevDescription = action.descItemResult.item.description;
                    }
                    if (action.descItemResult.item.refTemplateId) {
                        loc.descItem.prevRefTemplateId = action.descItemResult.item.refTemplateId;
                    }
                    loc.descItem.touched = false;
                    // Aktualizace position - pokud by create byl na první hodnotě a za ní již nějaké uživatel uložil, musí se vše aktualizovat
                    loc.descItemType.descItems.forEach((descItem, index) => {
                        descItem.position = index + 1;
                    });
                    newState = setLoc(newState, action.valueLocation, loc, true, true);
                    break;
                case 'DELETE_DESC_ITEM_TYPE':
                    // nic dalšího není potřeba, node se aktualizuje výše
                    break;
                default:
                    break;
            }
            return newState;

        case types.FUND_SUB_NODE_FORM_TEMPLATE_USE: {
            const { template: { formData, replaceValues }, groups } = action;
            console.log("use template", action.template);

            const { refTypesMap, infoTypesMap } = state;

            const currentFormData = state.formData
            let descItemGroups = [];
            let descItemTypes = [];

            currentFormData.descItemGroups.forEach((group) => {
                descItemGroups.push(group);
                group.descItemTypes.forEach((type) => {
                    descItemTypes.push(type);
                })
            })

            const updateDescItems = (itemType, newDescItems, replace = false) => {
                let descItems = itemType.descItems ? [...itemType.descItems] : [];

                // Replace values when 'replaceValues' is true, add values when none exist or itemType is repeatable.
                // Do nothing otherwise
                if (replace) {
                    descItems = [...newDescItems];
                } else if (descItems.length === 0 || itemType.rep) {
                    descItems.push(...newDescItems);
                }

                return { ...itemType, descItems };
            }

            Object.keys(formData).forEach((itemTypeId) => {
                itemTypeId = parseInt(itemTypeId);
                const descItemTypeIndex = descItemTypes.findIndex((type) => type.id === itemTypeId);
                let itemType = descItemTypeIndex !== -1 ? descItemTypes[descItemTypeIndex] : infoTypesMap[itemTypeId];

                const newDescItems = formData[itemTypeId].map((item) => ({
                    ...createDescItem(itemType, refTypesMap[itemTypeId], true),
                    ...item,
                }))

                itemType = updateDescItems(itemType, newDescItems, replaceValues);

                consolidateDescItems(itemType, infoTypesMap[itemTypeId], refTypesMap[itemTypeId], true);

                // if itemType exists, update values, if not, add it
                if (descItemTypeIndex !== -1) {
                    descItemTypes[descItemTypeIndex] = itemType;
                } else {
                    descItemTypes.push(itemType);
                }

                const groupCode = groups.reverse[itemTypeId];
                const descItemGroup = groups[groupCode];

                // if the group doesn't exist in the existing list, add it
                if (descItemGroups.find((group) => group.code === groupCode) === undefined) {
                    descItemGroups.push(descItemGroup);
                }
            })

            // order types by viewOrder
            descItemTypes = descItemTypes.sort((typeA, typeB) =>
                refTypesMap[typeA.id].viewOrder - refTypesMap[typeB.id].viewOrder
            )

            // order groups by code
            descItemGroups = descItemGroups.sort((groupA, groupB) => {
                if (groupA.code < groupB.code) { return -1 }
                if (groupA.code > groupB.code) { return 1 }
                return 0;
            })

            // update groups with new types
            descItemGroups = descItemGroups.map((group) => ({
                ...group,
                descItemTypes: descItemTypes.filter((type) => groups.reverse[type.id] === group.code)
            }))

            state.formData.descItemGroups = [...descItemGroups];
            checkFormData(state.formData);
            return { ...state };
        }

        // Přidá identifikátory typů atributů, které budou s dalším načtením obsahu JP přidány (prázdné)
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPES_ADD_TEMPLATE: {
            state.addItemTypeIds = action.itemTypeIds;
            checkFormData(state.formData);
            return { ...state };
        }

        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
            addItemType(state, action.descItemTypeId);
            checkFormData(state.formData);
            return { ...state };
        case types.FUND_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
            if (action.onlyDescItems) {
                // jen desc items, nic víc
                loc.descItemType.descItems = [];
            } else {
                var infoType = state.infoTypesMap[loc.descItemType.id];
                var refType = state.refTypesMap[loc.descItemType.id];

                state = {
                    ...state,
                    infoTypesMap: {
                        ...state.infoTypesMap,
                        [loc.descItemType.id]: {
                            ...infoType,
                            calSt: 0,
                        },
                    },
                };

                // Odebereme pouze pokud je pole jiné než: REQUIRED nebo RECOMMENDED
                if (
                    isType(infoType.type, RulItemTypeType.REQUIRED) ||
                    isType(infoType.type, RulItemTypeType.RECOMMENDED)
                ) {
                    // ponecháme, pouze odebereme hodnoty
                    // Hodnoty odebereme
                    loc.descItemType.descItems = [];

                    // Upravení a opravení seznamu hodnot, případně přidání prázdných
                    consolidateDescItems(loc.descItemType, infoType, refType, true);
                } else {
                    // kompletně odebereme
                    loc.descItemGroup.descItemTypes = [
                        ...loc.descItemGroup.descItemTypes.slice(0, action.valueLocation.descItemTypeIndex),
                        ...loc.descItemGroup.descItemTypes.slice(action.valueLocation.descItemTypeIndex + 1),
                    ];
                }
            }

            return setLoc(state, action.valueLocation, loc, false, false);
        case types.FUND_SUB_NODE_FORM_VALUE_DELETE:
            loc.descItemType.descItems = [
                ...loc.descItemType.descItems.slice(0, action.valueLocation.descItemIndex),
                ...loc.descItemType.descItems.slice(action.valueLocation.descItemIndex + 1),
            ];

            var infoType = state.infoTypesMap[loc.descItemType.id];
            var refType = state.refTypesMap[loc.descItemType.id];

            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(loc.descItemType, infoType, refType, true);

            return setLoc(state, action.valueLocation, loc, true, false);
        case types.FUND_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                fetchingId: action.nodeId,
                isFetching: true,
            });
        case types.FUND_SUB_NODE_FORM_RECEIVE: {
            const prevSTate = JSON.stringify(state, null, 4);
            window.prevSTATE = prevSTate;
            // ##
            // # Inicializace dat
            // ##

            // Doplnění descItemTypes o rulDataType a další data
            const dataTypeMap = getMapFromList(action.rulDataTypes.items);
            const descItemTypes = action.refDescItemTypes.items.map(type => {
                return {
                    ...type,
                    dataType: dataTypeMap[type.dataTypeId],
                    descItemSpecsMap: getMapFromList(type.descItemSpecs),
                    //viewDefinitionMap: type.viewDefinition ? getMapFromList(type.viewDefinition, "code") : null,
                };
            });

            // Sestavení mapy ref descItemType
            const refTypesMap = getMapFromList(descItemTypes);

            // ##
            // # Result a merge formuláře.
            // ##
            const result = {
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
            checkFormData(result.formData, '#checkFormData - 1');
            updateFormData(result, action.data, refTypesMap, action.groups, null, state.dirty);
            checkFormData(result.formData, '#checkFormData - 2');

            // Pokud existují typy atributů, které chceme po načtení přidat, přidáme je
            if (result.addItemTypeIds?.length > 0) {
                const itemTypeIds = {};
                // Group itemTypes
                (result.addItemTypeIds).forEach((itemTypeId) => {
                    if (!itemTypeIds[itemTypeId]) {
                        itemTypeIds[itemTypeId] = 1;
                    } else {
                        itemTypeIds[itemTypeId] = itemTypeIds[itemTypeId] + 1;
                    }
                })

                // Adds empty items from template for requested itemTypes
                Object.entries(itemTypeIds).forEach(([itemTypeId, emptyItemCount]) => {
                    let [descItemGroup, descItemType] = findGroupAndType(result.formData.descItemGroups, itemTypeId);

                    let existingCount = descItemType?.descItems?.length || 0;
                    let existingEmptyItemCount = descItemType?.descItems?.filter(
                        (descItem) => descItem.value == undefined
                            && (descItem.descItemSpecId == undefined || descItem.descItemSpecId == "")
                            && !descItem.undefined
                    ).length || 0;
                    // Subtract already existing empty items
                    let missingEmptyItemCount = emptyItemCount - existingEmptyItemCount;

                    while (missingEmptyItemCount > 0) {
                        if (existingCount !== 0) {
                            [descItemGroup, descItemType] = findGroupAndType(result.formData.descItemGroups, itemTypeId);
                            addValue(result, { descItemGroup, descItemType });
                        } else {
                            addItemType(result, itemTypeId);
                        }
                        missingEmptyItemCount--;
                        existingCount++;
                    }
                });
                result.addItemTypeIds = null;
            }
            checkFormData(result.formData, '#checkFormData - final');
            const postState = JSON.stringify(result, null, 4);
            window.postState = postState;
            return result;
        }
        case types.FUND_SUBNODE_UPDATE: {
            const { node, parent } = action.data;
            let nodeId = (node && node.id) || (parent && parent.id);

            if (nodeId !== state.nodeId) {
                // not the right node
                return state;
            }

            const result = {
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
        }
        case types.CHANGE_FUND_RECORD:
            return {
                ...state,
                data: {
                    ...state.data,
                    node: {
                        ...state.data.node,
                        version: action.version,
                    },
                },
            };
        default:
            return state;
    }
}
