import {i18n} from '../../../components/shared';
import {getMapFromList, objectById} from '../utils2'
import {validateCoordinatePoint, validateDouble, validateDuration, validateInt} from '../../../components/validate'
import {valuesEquals} from '../../../components/Utils'
import {DisplayType} from "../../../constants";
import {ItemAvailability, updateFormData, consolidateDescItems, createItem, createItemFromDb, mergeAfterUpdate} from "./itemFormUtils";
import {DataTypeCode} from "./itemFormInterfaces";
import * as types from '../../../actions/constants/ActionTypes.js'

export interface ILocation {
    itemType: ItemTypeExt;
    item: ApItemExt<any> | null;
}

export interface DescItemSpecLiteVO {
    id: number;
    type: number;
    rep: number;
}

export interface ItemTypeLiteVO {
    id: number;
    type: number;
    rep: number;
    cal: number;
    calSt: number;
    ind: number;
    specs: DescItemSpecLiteVO[];
    favoriteSpecIds: number[];
    width: number;

    // nad míru objektu
    hasFocus?: any;
    useSpecification?: boolean
}

export interface ApItemVO<V> {
    '@class': string | null,
    id?: number;
    objectId?: number;
    position?: number;
    typeId?: number;
    specId?: number;

    // nad míru objektu
    value: V;
    error: any;
    prevValue: V | null;
    prevDescItemSpecId?: number;
    calendarTypeId?: number;
    prevCalendarTypeId?: number;
    prevDescription?: string;
    description?: string;
    validateTimer?: any;
    touched?: any;
    visited?: any;
    hasFocus?: any;
    saving?: any;
    undefined?: boolean;
}

export interface ApItemExt<V> extends ApItemVO<V> {
    // value: V;
    // error: any;
    // prevValue: V | null;
    // descItemSpecId?: number;
    // prevDescItemSpecId?: number;
    // calendarTypeId?: number;
    // prevCalendarTypeId?: number;
    // validateTimer?: any;
    // touched?: any;
    // visited?: any;
    // hasFocus?: any;
    // saving?: any;
    // undefined?: boolean;
    formKey?: any;
    addedByUser: boolean;
    _uid?: string;
}

export interface ItemSpec {
    id: number;

}

export interface DataType {
    id: number;
    code: DataTypeCode;
}

export interface RefType {
    id: number,
    name: string;
    dataType: DataType;
    descItemSpecsMap: Map<number, ItemSpec>;
    useSpecification: boolean;
    viewDefinition: string;

    // server
    dataTypeId: number;
    code: string;
    shortcut: string;
    description: string;
    isValueUnique: boolean;
    canBeOrdered: boolean;
    viewOrder: number;
    width: number;
    structureTypeId: number;
    fragmentTypeId: number;
    descItemSpecs: ItemSpec[];

}

export interface FragmentTypeVO {
    id: number;
    code: string;
    name: string;
}

export interface RefTypeExt extends RefType {
    cal: number,
    calSt: number,
    favoriteSpecIds: any[],
    ind: number,
    rep: number,
    specs: any[],
    type: ItemAvailability,

    // nad míru objektu
    hasFocus?: any;
}

export interface ItemTypeExt extends ItemTypeLiteVO {
    items: ApItemExt<any>[]
}

export interface IFormData {
    itemTypes: ItemTypeExt[];
}

export interface ApFormVO {
    items: ApItemVO<any>[],
    itemTypes: ItemTypeLiteVO[],
}

export interface ItemData extends ApFormVO {
    parent: { id: number }
}

export interface IItemFormState {
    formData?: IFormData;
    isFetching: boolean;
    fetchingId?: number;
    fetched: boolean;
    dirty: boolean;
    needClean: boolean;
    versionId?: number;
    parent?: {id: number, [key: string]: any};
    data?: ItemData;
    infoTypes?: RefType[];
    infoTypesMap?: Map<any, RefTypeExt>;
    refTypesMap?: Map<any, RefType>;
    updatedItem?: ApItemExt<any>;
    getLoc: (state: IItemFormState, valueLocation: IValueLocation) => ILocation | null;
}

const initialState : IItemFormState = {
    isFetching: false,
    fetchingId: undefined,
    fetched: false,
    dirty: false,
    needClean: false,   // pokud je true, přenačtou se data a vymaže se aktuální editace - obdoba jako nové zobrazení formuláře
    versionId: undefined,
    parent: undefined,
    formData: undefined,
    data: undefined,
    infoTypesMap: undefined,
    refTypesMap: undefined,
    updatedItem: undefined,
    getLoc: getLoc
};

interface IValueLocation {
    itemTypeIndex: number
    itemIndex?: number
}

function getLoc(state: IItemFormState, valueLocation: IValueLocation) : ILocation | null {
    const formData = state.formData;
    if (!formData) {
        console.warn("formData do not exist");
        return null;
    }
    let itemType = formData.itemTypes[valueLocation.itemTypeIndex];
    let item : ApItemExt<any> | null = null;
    if (typeof valueLocation.itemIndex !== 'undefined') {
        item = itemType.items[valueLocation.itemIndex];
    }

    return {
        itemType,
        item
    }
}

interface IValidationError {
    spec?: string
    value?: string
    calendarType?: string
    hasError: boolean
}

export function validate(item: ApItemVO<any>, refType: RefType, valueServerError?: string) : IValidationError {
    const error :IValidationError = {hasError: false};

    // Specifikace
    if (refType.useSpecification) {
        if (typeof item.specId === 'undefined') {// || item.descItemSpecId === ''
            error.spec = i18n('subNodeForm.validate.spec.required');
        }
    }

    // Hodnota
    switch (refType.dataType.code) {
        case DataTypeCode.PARTY_REF:
        case DataTypeCode.RECORD_REF:
        case DataTypeCode.APFRAG_REF:
            if (!item.value || typeof item.value !== 'number') {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case DataTypeCode.URI_REF:
            if (!item.value || typeof item.value !== 'number') {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case DataTypeCode.STRUCTURED:
            if (!item.value || typeof item.value !== 'number') {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case DataTypeCode.JSON_TABLE:
            break;
        case DataTypeCode.ENUM:
            break;
        case DataTypeCode.UNITDATE:
            if (typeof item.calendarTypeId  == 'undefined') {// || item.calendarTypeId == ""
                error.calendarType = i18n('subNodeForm.validate.calendarType.required');
            }
            if (!item.value || item.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case DataTypeCode.UNITID:
            break;
        case DataTypeCode.FORMATTED_TEXT:
        case DataTypeCode.TEXT:
        case DataTypeCode.STRING:
            if (!item.value || item.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            break;
        case DataTypeCode.INT:
            if (!item.value || item.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                if (refType.viewDefinition === DisplayType.DURATION) {
                    error.value = validateDuration(item.value);
                } else {
                    error.value = validateInt(item.value);
                }
            }
            break;
        case DataTypeCode.COORDINATES:
            error.value = validateCoordinatePoint(item.value);
            break;
        case DataTypeCode.DECIMAL:
            if (!item.value || item.value.length === 0) {
                error.value = i18n('subNodeForm.validate.value.notEmpty');
            } else {
                error.value = validateDouble(item.value)
            }
            break;
        case DataTypeCode.DATE:
            if (!item.value || item.value.length === 0) {
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

    error.hasError = !!(error.spec || error.value || error.calendarType);

    return error;
}

/*
* Converts the value to specified type through the dataTypeMap
*/
export function convertValue(value, descItem, type) {
    //  Data type to value conversion functions map
    const dataTypeMap = {
        URI_REF: (value) => {
            return {
                touched: descItem.value !== value.value || descItem.description !== value.description,
                value: value.value,
                description: value.description
            }
        },
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

enum ActionOperation {
    DELETE = "DELETE",
    UPDATE = "UPDATE",
    CREATE = "CREATE",
    DELETE_DESC_ITEM_TYPE = "DELETE_DESC_ITEM_TYPE",
}

interface IAction {
    valueLocation?: IValueLocation,
    type?: string,
    readMode?: boolean,
    result?: {
        valid: boolean,
        message: string
    },
    index?: number,
    value?: any,
    operationType?: ActionOperation,
    descItemResult?: {
        item?: ApItemVO<any>
    },
    [extraProps: string]: any
}

export function itemForm(state: IItemFormState = initialState, action: IAction = {}) {
    // Načtení umístění, pokud bylo v akci předáno

    if (action.valueLocation) {
        // loc = getLoc(state, action.valueLocation);
    }

    if (action.valueLocation) {
        const loc = getLoc(state, action.valueLocation);

        if (loc) {
            const refType = state.refTypesMap!!.get(loc.itemType.id)!!;

            switch (action.type) {
                case types.ITEM_FORM_VALUE_VALIDATE_RESULT:
                    let valueServerError;
                    if (!action.result!!.valid) {
                        valueServerError = action.result!!.message;
                    }
                    loc.item!!.error = validate(loc.item!!, refType, valueServerError);

                    // state.formData = {...state.formData};
                    return {
                        ...state,
                        formData: {
                            ...state.formData,

                        }
                    };
                /*case types.ITEM_FORM_FORM_VALUE_CHANGE_POSITION:
                    const descItems = [
                        ...loc.itemType.descItems
                    ];

                    descItems.splice(action.index, 0, descItems.splice(action.valueLocation.descItemIndex, 1)[0]);

                    loc.itemType.descItems = descItems
                    state.formData = {...state.formData};
                    return {...state};*/
                case types.ITEM_FORM_VALUE_CHANGE:
                case types.ITEM_FORM_VALUE_CHANGE_PARTY:
                case types.ITEM_FORM_VALUE_CHANGE_RECORD:
                    // TODO přepsat na immutable
                    const convertedValue = convertValue(action.value, loc.item, refType.dataType.code);
                    // touched if new value is not equal with previous value, or something else changed during conversion
                    const touched = convertedValue.touched || !valuesEquals(loc.item!!.value, loc.item!!.prevValue);
                    loc.item = {
                        ...loc.item,
                        ...convertedValue,
                        touched
                    };
                    // Unitdate server validation
                    if(refType.dataType.code === "UNITDATE"){
                        if (loc.item!!.validateTimer) {
                            clearTimeout(loc.item!!.validateTimer);
                        }
                        // FIXME @randak tohle je blbě
                        const fc = () => action.dispatch(
                            action.formActions.fundSubNodeFormValueValidate(action.valueLocation)
                        );
                        loc.item!!.validateTimer = setTimeout(fc, 250);
                    }
                    loc.item!!.error = validate(loc.item!!, refType);

                    return {
                        ...state,
                        formData: {
                            ...state.formData
                        }
                    };
                case types.ITEM_FORM_VALUE_CHANGE_SPEC:
                    if (loc.item!!.specId !== action.value) {
                        loc.item!!.specId = action.value;
                        loc.item!!.touched = true;
                        loc.item!!.error = validate(loc.item!!, refType);

                        return {
                            ...state,
                            formData: {
                                ...state.formData
                            }
                        };
                    } else {
                        return state;
                    }
                case types.ITEM_FORM_VALUE_BLUR:
                    loc.item!!.hasFocus = false;
                    loc.itemType.hasFocus = false;


                    return {
                        ...state,
                        formData: {
                            ...state.formData
                        }
                    };
                case types.ITEM_FORM_VALUE_FOCUS:
                    loc.item!!.visited = true;
                    loc.item!!.hasFocus = true;
                    loc.itemType.hasFocus = true;

                    return {
                        ...state,
                        formData: {
                            ...state.formData
                        }
                    };
                case types.ITEM_FORM_VALUE_CREATE:
                    loc.item!!.saving = true;

                    return {
                        ...state,
                        formData: {
                            ...state.formData
                        }
                    };
                case types.ITEM_FORM_VALUE_ADD:
                    const item = createItem(loc.itemType, refType, true);
                    item.position = loc.itemType.items.length + 1;
                    //loc.itemType.items = [...loc.itemType.descItems, descItem];


                    return {
                        ...state,
                        formData: {
                            ...state.formData,
                            itemTypes: [
                                ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                {
                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                    items: [
                                        ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items,
                                        item
                                    ]
                                },
                                ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                            ]
                        }
                    };
                /*case types.ITEM_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:// TODO Asi nemáme
                    state.data.parent = action.copySiblingResult.node;

                    var currentDescItemMap = {}
                    loc.itemType.descItems.forEach(descItem => {currentDescItemMap[descItem.objectId] = descItem})
                    loc.itemType.descItems = action.copySiblingResult.type.descItems.map(descItem => {
                        var newDescItem = createItemFromDb(loc.itemType, descItem)
                        var currDescItem = currentDescItemMap[descItem.objectId]
                        if (currDescItem && currDescItem.hasFocus) {
                            newDescItem.hasFocus = true;
                        }
                        return newDescItem;
                    })

                    var infoType = state.infoTypesMap[loc.itemType.id]

                    // Upravení a opravení seznamu hodnot, případně přidání prázdných
                    consolidateDescItems(loc.itemType, infoType, refType, false)

                    state.formData = {...state.formData};
                    return {...state};*/
                case types.ITEM_FORM_OUTPUT_CALC_SWITCH: {
                    const infoType = state.infoTypesMap!![loc.itemType.id];
                    return {
                        ...state,
                        infoTypesMap: {
                            ...state.infoTypesMap,
                            [loc.itemType.id]: {
                                ...infoType,
                                calSt: 1 === infoType.calSt ? 0 : 1
                            }
                        }
                    }
                }

                case types.ITEM_FORM_VALUE_RESPONSE:
                    const newState = {
                        ...state,
                        data: {
                            ...state.data,
                        }
                    };

                    switch (action.operationType) {
                        case ActionOperation.DELETE:
                            // Aktualizace position
                            // loc.itemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
                            /*return {
                                ...newState,
                                formData: {
                                    ...newState.formData,
                                    itemTypes: [
                                        ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                        {
                                            ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                            items: state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.map((item, index) => ({...item, position: index + 1}))
                                        },
                                        ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                                    ]
                                }
                            };*/
                            // Aktualizuje nám to websocket
                            return state;
                        case ActionOperation.UPDATE:
                            if (action.descItemResult) {
                                const updateItem = state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items[action.valueLocation.itemIndex!!];
                                return {
                                    ...newState,
                                    formData: {
                                        ...newState.formData,
                                        itemTypes: [
                                            ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                            {
                                                ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                                items: [
                                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(0, action.valueLocation.itemIndex!!),
                                                    {
                                                        ...updateItem,
                                                        objectId: action.descItemResult.item ? action.descItemResult.item.objectId : null,
                                                        prevValue: action.descItemResult.item ? action.descItemResult.item.value : null,
                                                        //prevDescItemSpecId: action.descItemResult.item && loc.itemType.useSpecification ? action.descItemResult.item.descItemSpecId : updateItem.prevDescItemSpecId,
                                                        //prevCalendarTypeId: action.descItemResult.item && action.descItemResult.prevCalendarTypeId ? action.descItemResult.item.prevCalendarTypeId : updateItem.prevCalendarTypeId,
                                                        touched: false
                                                    },
                                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(action.valueLocation.itemIndex!!+1)
                                                ]
                                            },
                                            ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                                        ]
                                    }
                                };
                            }
                            return state;
                        case ActionOperation.CREATE:
                            if (action.descItemResult && action.descItemResult.item) {
                                const itemIndex = action.valueLocation.itemIndex!!;
                                const createItemxx = state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items[itemIndex];
                                const createItems = [
                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(0, itemIndex),
                                    {
                                        ...createItemxx,
                                        objectId: action.descItemResult.item.objectId,
                                        id: action.descItemResult.item.id,
                                        prevValue: action.descItemResult.item.value,
                                        //party: action.descItemResult.item.party,
                                        //record: action.descItemResult.item.record,
                                        //prevDescItemSpecId: loc.itemType.useSpecification ? action.descItemResult.item.descItemSpecId : undefined,
                                        prevCalendarTypeId: action.descItemResult.item.calendarTypeId || undefined,
                                        prevDescription: action.descItemResult.item.description || undefined,
                                        saving: false,
                                        touched: false
                                    },
                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(itemIndex+1)
                                ];

                                return {
                                    ...newState,
                                    formData: {
                                        ...newState.formData,
                                        itemTypes: [
                                            ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                            {
                                                ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                                items: createItems.map((item, index) => ({...item, position: index})) // Aktualizace position - pokud by create byl na první hodnotě a za ní již nějaké uživatel uložil, musí se vše aktualizovat
                                            },
                                            ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                                        ]
                                    }
                                };

                            }
                            return state;
                        case ActionOperation.DELETE_DESC_ITEM_TYPE:
                            // Aktualizuje nám to websocket
                            return state;
                    }

                    return state;
                case types.ITEM_FORM_DESC_ITEM_TYPE_DELETE:

                    if (action.onlyDescItems) { // jen desc items, nic víc
                        return {
                            ...state,
                            formData: {
                                ...state.formData,
                                itemTypes: [
                                    ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                    {
                                        ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                        items: []
                                    },
                                    ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                                ]
                            }
                        };
                    } else {
                        /*var infoType = state.infoTypesMap[loc.itemType.id]

                        // Odebereme pouze pokud je pole jiné než: REQUIRED nebo RECOMMENDED
                        if (infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED') { // ponecháme, pouze odebereme hodnoty
                            // Hodnoty odebereme
                            loc.itemType.descItems = [];

                            // Upravení a opravení seznamu hodnot, případně přidání prázdných
                            consolidateDescItems(loc.itemType, infoType, refType, true)
                        } else { // kompletně odebereme
                            loc.itemGroup.descItemTypes = [
                                ...loc.itemGroup.descItemTypes.slice(0, action.valueLocation.descItemTypeIndex),
                                ...loc.itemGroup.descItemTypes.slice(action.valueLocation.descItemTypeIndex + 1)
                            ]
                        }*/
                    }

                    return {
                        ...state,
                        formData: {
                            ...state.formData
                        }
                    };
                case types.ITEM_FORM_VALUE_DELETE:
                    // loc.itemType.descItems = [
                    //     ...loc.itemType.descItems.slice(0, action.valueLocation.descItemIndex),
                    //     ...loc.itemType.descItems.slice(action.valueLocation.descItemIndex + 1)
                    // ];

                    // TODO konsolidace prázdných
                    // var infoType = state.infoTypesMap[loc.itemType.id]

                    // Upravení a opravení seznamu hodnot, případně přidání prázdných
                    // consolidateDescItems(loc.itemType, infoType, refType, true)
                    return {
                        ...state,
                        formData: {
                            ...state.formData,
                            itemTypes: [
                                ...state.formData!!.itemTypes.slice(0, action.valueLocation.itemTypeIndex),
                                {
                                    ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex],
                                    items: [
                                        ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(0, action.valueLocation.itemIndex!!),
                                        ...state.formData!!.itemTypes[action.valueLocation.itemTypeIndex].items.slice(action.valueLocation.itemIndex!! + 1)
                                    ]
                                },
                                ...state.formData!!.itemTypes.slice(action.valueLocation.itemTypeIndex+1),
                            ]
                        }
                    };
            }

        }


    }

    switch (action.type) {
        case types.ITEM_FORM_DESC_ITEM_TYPE_ADD:
            // Dohledání skupiny a desc item type
            // var addGroup, addItemType;
            // state.infoGroups.forEach(group => {
            //     group.types.forEach(type => {
            //         if (type.id == action.descItemTypeId) {
            //             addGroup = group;
            //             addItemType = type;
            //         }
            //     });
            // });



            const addItemType = objectById(state.data!!.itemTypes, action.descItemTypeId) as ItemTypeExt;

            // ##
            // # Přidání do formuláře
            // ##

            // Dohledání skupiny, pokud existuje


            // Přidání prvku do skupiny a seřazení prvků podle position
            const descItemType = {...addItemType, items: []};
            // Musíme ponechat prázdnou hodnotu
            const infoType = state.infoTypesMap!!.get(descItemType!!.id)!!;
            const refType = state.refTypesMap!!.get(descItemType!!.id)!!;

            // TODO consolidace
            // Upravení a opravení seznamu hodnot, případně přidání prázdných
            consolidateDescItems(descItemType, infoType, refType, true);


            const itemTypes = [
                ...state.formData!!.itemTypes,
                descItemType
            ];

            itemTypes.sort((a, b) => {
                return state.refTypesMap!!.get(a.id)!!.viewOrder - state.refTypesMap!!.get(b.id)!!.viewOrder
            });

            return {
                ...state,
                formData: {
                    ...state.formData,
                    itemTypes,
                }
            };
        case types.CHANGE_ACCESS_POINT:
            return {...state, dirty: true};
        case types.ITEM_FORM_CHANGE_READ_MODE:
            if (action.readMode) {  // změna na read mode - musíme vyresetovat všechny změny ve formuláři
                return {
                    ...state,
                    needClean: true,
                };
            } else {
                return state;
            }
            // tuhle funkcionalitu nechceme
        /*case types.ITEM_FORM_TEMPLATE_USE: {
            console.warn("ITEM_FORM_TEMPLATE_USE", action, state);

            const groups = action.groups;
            const template = action.template;
            const formData = template.formData;
            const replaceValues = template.replaceValues;

            Object.keys(formData).map(itemTypeId => {
                let existsItemType = false;
                const items = formData[itemTypeId];
                console.warn(itemTypeId, items);
                const groupCode = groups.reverse[itemTypeId];
                const group = groups[groupCode];

                const addItemType = state.infoTypesMap!![itemTypeId];

                // Dohledání skupiny, pokud existuje
                // const grpIndex = indexById(state.formData.descItemGroups, groupCode, 'code');

                let itemsMerge = [];

                // Přidání prvku do skupiny a seřazení prvků podle position
                const itemType = {...addItemType, descItems: itemsMerge};
                if (!existsItemType) {
                    // descItemGroup.descItemTypes.push(itemType);
                }
                // Musíme ponechat prázdnou hodnotu
                const refType = state.refTypesMap!![itemType.id];
                const infoType = state.infoTypesMap!![itemType.id];

                // Upravení a opravení seznamu hodnot, případně přidání prázdných
                consolidateDescItems(itemType, infoType, refType, true);

                // descItemGroup.descItemTypes.sort((a, b) => {
                //     return state.refTypesMap[a.id].viewOrder - state.refTypesMap[b.id].viewOrder
                //     return a.viewOrder - b.viewOrder
                // });

            });

            return {
                ...state,
                formData: {
                    ...state.formData
                }
            };
        }*/


        case types.ITEM_FORM_REQUEST:
            return {
                ...state,
                parent: action.parent,
                fetchingId: action.parent.id,
                isFetching: true,
            };
        case types.ITEM_FORM_RECEIVE:
            // ##
            // # Inicializace dat
            // ##
            if (action.parent.id !== state.parent!!.id) {
                return state;
            }

            // Doplnění descItemTypes o rulDataType a další data
            const dataTypeMap = getMapFromList(action.rulDataTypes.items);
            const descItemTypes = action.refDescItemTypes.items.map(type => {
                return {
                    ...type,
                    dataType: dataTypeMap.get(type.dataTypeId),
                    descItemSpecsMap: getMapFromList(type.descItemSpecs),
                    //viewDefinitionMap: type.viewDefinition ? getMapFromList(type.viewDefinition, "code") : null,
                }
            }) as RefTypeExt[];

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
                id: action.id,
                needClean: false,
            };

            // Pokud je vyžadován reset formuláře, nastavíme předchozí data na null a tím se vše znovu nainicializuje
            if (action.needClean) {
                result.data = undefined;
                result.formData = undefined;
            }
            return updateFormData(result, action.data, refTypesMap, state.dirty);
        case types.FUND_SUBNODE_UPDATE:
            var {node, parent} = action.data;
            let nodeId = (node && node.id) || (parent && parent.id);

            if (nodeId != state.parent!!.id){
                // not the right node
                return state;
            }

            const resultUpdate = {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                versionId: action.versionId,
                nodeId: nodeId,
                needClean: false,
            };

            mergeAfterUpdate(resultUpdate, action.data, action.refTables); // merges result with data from action

            return resultUpdate;
        default:
            return state
    }
}

