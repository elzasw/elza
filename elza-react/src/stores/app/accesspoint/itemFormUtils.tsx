import { hasDescItemTypeValue } from '../../../components/arr/ArrUtils';
import { toDuration } from '../../../components/validate';
import { DisplayType } from '../../../constants';
import { getMapFromList, indexById } from '../utils2';
import {
    ApItemExt,
    ApItemVO,
    IFormData,
    IItemFormState,
    ItemData,
    ItemTypeExt,
    ItemTypeLiteVO,
    RefType,
    RefTypeExt,
} from './itemForm';
import { DataTypeCode } from './itemFormInterfaces';

export enum ItemAvailability {
    REQUIRED= "REQUIRED",
    RECOMMENDED= "RECOMMENDED",
    POSSIBLE= "POSSIBLE",
    IMPOSSIBLE= "IMPOSSIBLE"
}

export const ItemAvailabilityNumToEnumMap = {
    [3]: ItemAvailability.REQUIRED,
    [2]: ItemAvailability.RECOMMENDED,
    [1]: ItemAvailability.POSSIBLE,
    [0]: ItemAvailability.IMPOSSIBLE,
};



/**
 * Vytvoření map na základě - descItemGroup - code, descItemType - id a descItem - objectId.
 * Mapa se vytváří na základě již existujícího formuláře, pokud existuje.
 * groupMap - mapa group.code na group
 * typeMap - mapa type.id na type
 * itemMap - mapa item.objectId na item
 */
function createDataMap(formData: IFormData) {
    const typeMap = new Map<any, ItemTypeExt>();
    const itemMap = new Map<any, ApItemExt<any>>();

    if (formData) { // nějaká data již existují
        formData.itemTypes.forEach(type => {
            typeMap.set(type.id, type);

            type.items.forEach(item => {
                itemMap.set(item.objectId!!, item);
            })
        })
    }

    const get = (ths, key) => {
        return ths[key]
    };

    const find = (ths, key) => {
        return typeof ths[key] !== 'undefined' ? ths[key] : {}
    };

    // typeMap.get = get.bind(null, typeMap);
    // itemMap.get = get.bind(null, itemMap);
    // typeMap.find = find.bind(null, typeMap);
    // itemMap.find = find.bind(null, itemMap);

    return {
        typeMap,
        itemMap,
    }
}

export function createImplicitItem(descItemType: ItemTypeLiteVO, refType : RefType, addedByUser: boolean = false) : ApItemExt<any> {
    const descItem = createItem(descItemType, refType, addedByUser);
    descItem.position = 1;
    return descItem;
}

export function getFocusDescItemLocation(subNodeFormStore) {
    const formData = subNodeFormStore.formData;

    for (let g=0; g<formData.descItemGroups.length; g++) {
        const group = formData.descItemGroups[g];
        if (group.hasFocus) {
            for (let dit=0; dit<group.descItemTypes.length; dit++) {
                const descItemType = group.descItemTypes[dit];
                if (descItemType.hasFocus) {
                    for (let di=0; di<descItemType.descItems.length; di++) {
                        const descItem = descItemType.descItems[di];
                        if (descItem.hasFocus) {
                            return {
                                descItemGroupIndex: g,
                                descItemTypeIndex:  dit,
                                descItemIndex: di,
                            }
                        }
                    }
                }
            }
        }
    }

    return null
}

export function createItemFromDb(descItemType, descItem) {
    const result = {
        ...descItem,
        prevDescItemSpecId: descItem.specId,
        prevValue: descItem.value,
        hasFocus: false,
        touched: false,
        visited: false,
        error: {hasError: false}
    };

    initFormKey(descItemType, result);

    return result
}

function prevItemHasSamePrevValue(prevDescItem, newDescItem) {
    return prevDescItem.prevValue === newDescItem.value && prevDescItem.prevDescItemSpecId === newDescItem.specId
}

function addUid(descItem, index) {
    if (typeof descItem.objectId !== 'undefined') {
        descItem._uid = descItem.objectId;
    } else {
        descItem._uid = "_i" + index;
    }
}

const _formKeys = {};
function initFormKey(descItemType, descItem) {
    if (descItem.formKey) {
        return
    }

    if (!_formKeys[descItemType.id]) {
        _formKeys[descItemType.id] = 1
    }
    descItem.formKey = 'fk_' + _formKeys[descItemType.id];
    _formKeys[descItemType.id] = _formKeys[descItemType.id] + 1
}

// 1. Doplní povinné a doporučené specifikace s prázdnou hodnotou, pokud je potřeba
// 2. Pokud atribut nemá žádnou hodnotu, přidá první implicitní
//
export function consolidateDescItems(resultDescItemType: ItemTypeExt, infoType, refType: RefType, addedByUser: boolean, emptySystemSpecToKeyMap = new Map<number, string>()) {
    const forceVisibility = infoType.type == ItemAvailability.REQUIRED || infoType.type == ItemAvailability.RECOMMENDED;

    // Vynucené hodnoty se specifikací, pokud je potřeba
    addForcedSpecifications(resultDescItemType, infoType, refType, emptySystemSpecToKeyMap);

    // Přidáme jednu hodnotu - chceme i u opakovatelného, pokud žádnou nemá (nebyla hodnota přidána vynucením specifikací)
    if (resultDescItemType.items.length === 0) {
        resultDescItemType.items.push(createImplicitItem(resultDescItemType, refType, addedByUser));
    }

    if (refType.dataType.code === 'INT' && refType.viewDefinition === DisplayType.DURATION) {
        resultDescItemType.items.forEach(descItem => {
            if (!isNaN(descItem.prevValue)) {
                descItem.prevValue = toDuration(descItem.prevValue);
            }
            if (!isNaN(descItem.value)) {
                descItem.value = toDuration(descItem.value);
            }
        });
    }

    resultDescItemType.items.forEach((descItem, index) => {descItem.position = index + 1});
}

/**
 * Doplnění prázdných hodnot se specifikací, které jsou vynucené podle typu (REQUIRED a RECOMMENDED), pokud ještě v resultDescItemType nejsou.
 * Uvažujeme POUZE descItemType, které mají specifikaci a MAJÍ i hodnotu, né pouze specifikaci.
 */
export function addForcedSpecifications(resultDescItemType: ItemTypeExt, infoType, refType, emptySystemSpecToKeyMap = new Map<number, string>()) {
    if (!refType.useSpecification) {
        return
    }

    if (!hasDescItemTypeValue(refType.dataType)) {
        return
    }

    // Seznam existujících specifikací
    const existingSpecIds = new Set<number>();
    resultDescItemType.items.forEach(descItem => {
        if (descItem.specId && !existingSpecIds.has(descItem.specId)) {
             existingSpecIds.add(descItem.specId);
        }
    });

    infoType.specs.forEach(spec => {
        const infoSpec = infoType.descItemSpecsMap.get(spec.id);
        const forceVisibility = infoSpec.type == ItemAvailability.REQUIRED || infoSpec.type == ItemAvailability.RECOMMENDED;
        if (forceVisibility && !existingSpecIds.has(spec.id)) {  // přidáme ji na formulář, pokud má být vidět a ještě na formuláři není
            const descItem = createImplicitItem(resultDescItemType, refType, false);
            descItem.specId = spec.id;

            // Ponechání původního form key, pokud existovala tato položka již na klientovi a nebylo na ní šáhnuto
            const formKey = emptySystemSpecToKeyMap.get(spec.id);
            if (formKey) {
                descItem.formKey = formKey
            }

            // U vícehodnotových přidáváme všechny, které neexistují, u jednohodnotového nesmí být více než jedna
            if (infoType.rep === 1) {   // Vícehodnotový
                resultDescItemType.items.push(descItem)
            } else {    // Jednohodnotový, přidáme jen jednu
                if (resultDescItemType.items.length === 0) {    // není žádná, přidáme první
                    resultDescItemType.items.push(descItem)
                }
            }
        }
    })
}

function mergeDescItems(state: IItemFormState, resultDescItemType: ItemTypeExt, prevType?: ItemTypeExt, newType?: ItemTypeExt) {
    const infoType = state.infoTypesMap!!.get(resultDescItemType.id)!!;
    const refType = state.refTypesMap!!.get(resultDescItemType.id)!!;
    const forceVisibility = infoType.type == ItemAvailability.REQUIRED || infoType.type == ItemAvailability.RECOMMENDED;

    if (!prevType) {    // ještě ji na formuláři nemáme
        if (!newType) { // není ani v DB, přidáme ji pouze pokud je nastaveno forceVisibility
            if (forceVisibility) {  // přidáme ji pouze pokud je nastaveno forceVisibility
                // Upravení a opravení seznamu hodnot, případně přidání rázdných
                consolidateDescItems(resultDescItemType, infoType, refType, false);

                return true;
            }
        } else {    // je v db a není předchozí, dáme ji do formuláře bez merge
            newType.items.forEach(descItem => {
                resultDescItemType.items.push(createItemFromDb(resultDescItemType, descItem))
            });
            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            consolidateDescItems(resultDescItemType, infoType, refType, false);

            return true;
        }
    } else {    // již ji na formuláři máme, musíme provést merge
        if (!newType) { // není ani v DB, my jí máme, musíme nechat jen nově přidané hodnoty, protože ostatní i mnou editované již někdo smazal (protože nepřišel objekt newType)
            prevType.items.forEach(descItem => {
                if (typeof descItem.id === 'undefined' && descItem.addedByUser) { // mnou přidaná ještě neuložená, necháme je
                    resultDescItemType.items.push(descItem);
                }
            });

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            if (forceVisibility) {
                const count = resultDescItemType.items.length;
                consolidateDescItems(resultDescItemType, infoType, refType, false);

                // Oprava incrementování formKey - nechceme zvýšit formKey v případě že se nic nezměnilo (předchozí položka není na serveru a je vynucená nová také)
                if (count === 0 && resultDescItemType.items.length === 1 && prevType.items.length === 1 && prevType.items[0].formKey && !prevType.items[0].id) {
                    resultDescItemType.items[0].formKey = prevType.items[0].formKey;
                }
            }

            // Chceme ji pokud má nějaké hodnoty
            if (resultDescItemType.items.length > 0) {
                return true
            }
        } else {    // je v db a my ji také máme, musíme provést merge
            // Vezmeme jako primární nově příchozí hodnoty a do nich přidáme ty, které aktualní klient má přidané, ale nemá je ještě uložené např. kvůli validaci atp.
            // Pokud ale má klient ty samé hodnoty (prev value je stejné jako nově příchozí hodnota), jako přijdou ze serveru a současně je upravil a nejsou uložené, necháme hodnoty v našem klientovi

            // Mapa existujících hodnot na klientovi
            const prevDescItemMap = new Map<number, ApItemExt<any>>();
            prevType.items.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') { // hodnota již dříve přijatá ze serveru
                    prevDescItemMap.set(descItem.objectId!!, descItem);
                }
            });

            // Nakopírování nově přijatých hodnot, případně ponechání stejných (na základě objectId a prev value == value ze serveru, které již uživatel upravil a nejsou odeslané)
            newType.items.forEach(descItem => {
                const prevDescItem = prevDescItemMap.get(descItem.objectId!!);
                if (prevDescItem && (prevItemHasSamePrevValue(prevDescItem, descItem) && prevDescItem.touched || (!descItem.value && !descItem.undefined))) {   // původní hodnota přijatá ze serveru má stejné hodnoty jako jsou nyní v nově přijatých datech na serveru a uživatel nám aktuální data upravil
                    const item = prevDescItem;
                    if(state.updatedItem && (state.updatedItem.objectId === descItem.objectId)){
                        item.value = state.updatedItem.value;
                    }
                    addUid(item, null);
                    item.formKey = prevDescItem.formKey;
                    resultDescItemType.items.push(item)
                } else {
                    const item = createItemFromDb(resultDescItemType, descItem);
                    addUid(item, null);
                    if (prevDescItem) {
                        item.formKey = prevDescItem.formKey
                    } else {
                        initFormKey(resultDescItemType, item)
                    }
                    resultDescItemType.items.push(item)
                }
            });

            // Doplnění o přidané a neuložené v aktuálním klientovi
            // Pokud se jedná o jednohodnotvý atribut, necháme jen tu ze serveru
            const emptySystemSpecToKeyMap = new Map<number, string>();   // mapa id specifikace prázdné systémové vynucené položky specifikace na formKey dané položky - aby nám položky na formuláři neskákaly
            if (infoType.rep === 1) {   // Vícehodnotový
                let prevDescItem : ApItemExt<any> | null = null;
                prevType.items.forEach((descItem, index) => {
                    addUid(descItem, index);

                    if (typeof descItem.id === 'undefined') { // mnou přidaná ještě neuložená, musíme jí přidat na správné místo
                        // Pokud se jedná o systémově přidanou hodnotu a uživatel na ní zatím nešáhl, nebudeme ji vůbec uvažovat
                        if (!descItem.addedByUser && !descItem.touched) {    // systémově přidaná a neupravená
                            // nebudeme ji uvažovat, jen se pro ni budeme snažit zachovat formKey, aby nám položky na formuláři neskákaly - jedná se o systémnově přidané atributy s povinnou nebo doporučenou specifikací
                            if (refType.useSpecification && hasDescItemTypeValue(refType.dataType)) {
                                emptySystemSpecToKeyMap.set(descItem.specId!!, descItem.formKey)
                            }
                        } else {
                            if (prevDescItem) { // má předchozí, zkusíme ji v novém rozložení dát na stejné místo, pokud to půjde
                                const index = indexById(resultDescItemType.items, prevDescItem._uid, '_uid');
                                if (index !== null) {   // našli jsme položku, za kterou ji můžeme přidat
                                    resultDescItemType.items = [
                                        ...resultDescItemType.items.slice(0, index + 1),
                                        descItem,
                                        ...resultDescItemType.items.slice(index + 1)
                                    ]
                                } else {    // nenašli jsme položku, za kterou ji můžeme přidat, dáme ji na konec
                                    resultDescItemType.items.push(descItem);
                                }
                            } else {    // nemá předchozí, dáme ji v novém rozložení na konec
                                resultDescItemType.items.push(descItem);
                            }
                        }
                    }

                    prevDescItem = descItem;
                })
            }

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            consolidateDescItems(resultDescItemType, infoType, refType, false, emptySystemSpecToKeyMap);

            return true;
        }
    }

    // Uměle doplníme ty specifikace, které

    return false;
}
/**
 * Prepares flat form data.
 * Replacing availability ids with strings.
 * Inserting DescItemSpecsMap into types
 *
 * @param FlatFormData data
 *
 * @return FlatFormData
 */
function prepareFlatData(data){
    data.types = replaceIdsWithString(data.types, ItemAvailabilityNumToEnumMap);
    data.specs = replaceIdsWithString(data.specs, ItemAvailabilityNumToEnumMap);
    data.types = insertDescItemSpecsMap(data.types,data.specs);
    return data;
}
/**
 * Adds item from response as descItem
 *
 * @param FlatFormData data
 * @param Object item
 */
function addChangedItemIfExists(data, item){
    if(item){

        if(!data.descItems){
            data.descItems = {};
        }
        let itemId = item.objectId;
        data.descItems[itemId] = item;
        data.descItems.ids.push(itemId);
    }
}

type idsObject = {ids:any[]}

/**
 * Gets descItem ids per type as map.
 *
 * @param {Object} items
 *
 * @return Object
 */
function getMapByItemType(items){
    let types: idsObject = {ids:[]};

    for(let i = 0; i < items.ids.length; i++){
        let itemId = items.ids[i];
        let item = items[itemId];
        let typeId = item.itemType;
        if(!types[typeId]){
            types[typeId] = {
             items: []
            };
            types.ids.push(typeId);
        }
        types[typeId].items.push(itemId);
    }
    return types;
}
/**
 * Inserts item type into item
 *
 * @param {Object} item
 * @param {Object} items
 *
 * @return Object
 */
function insertItemType(item, items){
    item = {
        ...item,
        itemType: items[item.objectId].itemType
    };
    return item;
}

export function mergeAfterUpdate(state, data, refTables) {
    let changedItem = data.item;
    let flatForm = new FlatFormData(refTables);
    let flatLocalForm = new FlatFormData(refTables);

    // Initization of the flat forms
    flatForm.flattenInit(data);
    flatLocalForm.flattenInit(state.formData);

    // Modifications of the flat forms
    flatForm = prepareFlatData(flatForm);
    flatLocalForm = prepareFlatData(flatLocalForm);

    // Inserting item type from local descItems,
    // because it is not defined on the item received from server
    changedItem = insertItemType(changedItem, flatLocalForm.descItems);
    addChangedItemIfExists(flatForm, changedItem);

    flatLocalForm.update(flatForm);

    // Update info about descItemTypes
    state.infoTypesMap = flatLocalForm.types;
    // Update form with new data
    state.formData = restoreFormDataStructure(flatLocalForm);

    return state;
}
/**
 * Inserts map of specifications for descItems
 */
function insertDescItemSpecsMap(types, specs){
    for(let s = 0; s < specs.ids.length; s++){

        let specId = specs.ids[s];
        let spec = specs[specId];
        let type = types[spec.itemType];

        if(type){

            if(!type.descItemSpecsMap){
                type.descItemSpecsMap = {};
            }

            type.descItemSpecsMap[spec.id] = spec;
        }
    }
    return types;
}
/**
 * Recreate the original deep formData structure from flat data
 *
 * @param {FlatFormData} data
 *
 * @return Object
 * */
function restoreFormDataStructure(data: FlatFormData) : IFormData {
    let groupId, group, typeId, type, descItemId, descItem, specId, spec;
    let usedTypes : idsObject = {ids:[]};
    let itemTypes = [];

    for(let d = 0; d < data.descItems.ids.length; d++){
        descItemId = data.descItems.ids[d];
        descItem = data.descItems[descItemId];

        if(!usedTypes[descItem.itemType]){
            type = data.types[descItem.itemType];
            type.descItems = [];
            type.specs = [];
            usedTypes[descItem.itemType] = type;
            usedTypes.ids.push(descItem.itemType);
        }
        usedTypes[descItem.itemType].descItems.push(descItem);
    }

    for(let s = 0; s < data.specs.ids.length; s++){
        specId = data.specs.ids[s];
        spec = data.specs[specId];

        if(usedTypes[spec.itemType]){
            usedTypes[spec.itemType].specs.push(spec);
        }
    }

    for(let t = 0; t < usedTypes.ids.length; t++){
        typeId = usedTypes.ids[t];
        type = usedTypes[typeId];

        // if(!usedGroups[type.group]){
        //     group = data.groups[type.group];
        //     group.descItemTypes = [];
        //     usedGroups[type.group] = group;
        //     usedGroups.ids.push(type.group);
        // }
        // usedGroups[type.group].descItemTypes.push(type);
    }
    //
    // for(let g = 0; g < usedGroups.ids.length; g++){
    //     groupId = usedGroups.ids[g];
    //     group = usedGroups[groupId];
    //     descItemGroups.push(group);
    // }

    return {
        itemTypes
    };
}

class FlatFormData{
    _emptyItemCounter: number;
    descItems: { ids: any[]; };
    types: { ids: number[]; };
    refTables: any;
    specs: { ids: number[]; };

    /*
     * Example of the flat form data structure
     * {
           groups: {
               GROUP01: {
                   ...
                   code: "GROUP01",
                   name: "Skupina"
               },
               ids: ["GROUP01"]
           },
           types:{
               "1": {
                   ...
                   group: "GROUP01"
               },
               ids: ["1"]
           },
           descItems:{
               "1": {
                   ...
                   type: "1",
               },
               ids: ["1"]
           },
           specs:{
               "1": {
                   ...
                   type: "1"
               },
               ids: ["1"]
           }
       }
    */


    constructor(refTables){
        this._emptyItemCounter = 0;
        this.types = {ids:[]};
        this.descItems = {ids:[]};
        this.specs = {ids:[]};
        this.refTables = refTables;
    }
    /**
     * Loads form data
     *
     * @param data
     */
    init(data){
        this.types = data.types;
        this.descItems = data.descItems;
        this.specs = data.specs;
    }
    /**
     * Flattens and loads form data
     *
     * @param data
     */
    flattenInit(data){
        this._flattenFormData(data);
    }
    /**
     * Updates current form data with given form data
     *
     * @param {Object} newData
     */
    update(newData){
        this.types = newData.types;
        this.specs = newData.specs;

        this._updateDescItems(newData.descItems);
    }
    /**
     * Updates descItems with given items.
     *
     * @param {Object} newItems
     */
    _updateDescItems(newItems){
        this._mergeDescItems(newItems);
        this._deleteUnusedItems();
        this._generateNewItems();
    }
    /**
     * Merges given items into instance's descItems.
     * Modifies this.descItems.
     *
     * @param {Object} newItems
     */
    _mergeDescItems(newItems){
        let items = this.descItems;
        let types = this.types;

        for(let i = 0; i < newItems.ids.length; i++){
            let newItemId = newItems.ids[i];
            let item = items[newItemId];
            let newItem = newItems[newItemId];

            if(!item){
                items.ids.push(newItem.objectId);
            }
            if(item.prevValue !== newItem.value){
                newItem.value = item.value;
            }
            newItem = createItemFromDb(types[newItem.itemType], newItem);
            items[newItemId] = newItem;
        }
        this.descItems = items;
    }
    /**
     * Deletes unused items (items that are empty, and not added by user).
     */
    _deleteUnusedItems(){
        let items = this.descItems;
        let newIds = [...items.ids];

        for(let i = 0; i < items.ids.length; i++){
            const itemId = items.ids[i];
            const item = items[itemId];
            const isEmpty = item.value === null || item.specId === null;
            const type = this.types[item.itemType];
            const isFromDb = item.objectId >= 0;

            if(isEmpty && !item.addedByUser && !isFromDb){
                delete items[itemId];
                newIds.splice(newIds.indexOf(itemId),1);
            }
        }
        items.ids = newIds;
        this.descItems = items;
    }
    /**
     * Generates new object with desc items. Adds REQUIRED and RECOMMENDED item types and specifications.
     */
    _generateNewItems(){
        const types = this.types;
        const items = this.descItems;
        const specs = this.specs;
        const itemTypesMap= getMapByItemType(items);
        const itemSpecsMap = this._getForcedSpecsByType(specs);
        const refTypesMap = getMapFromList(this.refTables.descItemTypes.items) as any as Map<number, RefType>;
        const refDataTypesMap = getMapFromList(this.refTables.rulDataTypes.items);
        const newItems : idsObject = {ids:[]};

        for(let t = 0; t < types.ids.length; t++){
            let typeId = types.ids[t];
            let type = types[typeId];
            let forceVisible = type.type === ItemAvailability.REQUIRED || type.type === ItemAvailability.RECOMMENDED;
            let typeItems = itemTypesMap[typeId] && itemTypesMap[typeId].items;

            if (forceVisible){
                let refType = refTypesMap[typeId];
                refType.dataType = refDataTypesMap[refType.dataTypeId];
                let newItem;
                let nextEmptyItemIdBase = "item_";
                let nextEmptyItemId = nextEmptyItemIdBase + this._emptyItemCounter;
                let forcedTypeSpecs = itemSpecsMap[typeId] && itemSpecsMap[typeId].specs;

                //Add forced specifications
                if (forcedTypeSpecs){
                    let lastPosition = typeItems ? typeItems.length : 0;
                    let unusedForcedSpecs = this._getUnusedSpecIds(forcedTypeSpecs, typeItems);

                    for(let s = 0; s < unusedForcedSpecs.length; s++){
                        nextEmptyItemId = nextEmptyItemIdBase + this._emptyItemCounter;
                        newItem = createItem(type, refType, false);
                        newItem.specId = unusedForcedSpecs[s];
                        newItem.itemType = typeId;
                        newItem.position = lastPosition + 1;
                        lastPosition++;

                        newItems[nextEmptyItemId] = newItem;
                        newItems.ids.push(nextEmptyItemId);
                        this._emptyItemCounter++;
                    }
                }
                //Add forced itemTypes
                else if(!typeItems){
                    newItem = createItem(type, refType, false);
                    newItem.position = 1;
                    newItem.itemType = typeId;

                    newItems[nextEmptyItemId] = newItem;
                    newItems.ids.push(nextEmptyItemId);
                    this._emptyItemCounter++;
                }
            }
            if (typeItems){
                //Add existing items
                for(let i = 0; i < typeItems.length; i++){
                    const itemId = typeItems[i];
                    const item = items[itemId];

                    newItems[itemId] = item;
                    newItems.ids.push(itemId);
                }
            }
        }

        this.descItems = newItems;
    }
    /**
     * Returns specs from given array, that are not used in descItems
     *
     * @param {Array} specIds - Array of spec ids
     * @param {Array} itemIds - Array of descItem ids
     *
     * @return Array
     */
    _getUnusedSpecIds(specIds = this.specs.ids, itemIds = this.descItems.ids){
        let unusedSpecIds = [...specIds];
        for(let i = 0; i < itemIds.length; i++){
            let itemId = itemIds[i];
            let item = this.descItems[itemId];
            let specIndex = unusedSpecIds.indexOf(item.specId);
            if(specIndex >= 0){
                unusedSpecIds.splice(specIndex, 1);
            }
        }
        return unusedSpecIds;
    }
    /**
     * Returns Object of required or recommended spec ids (in array), mapped to item type ids
     * Ex.: {"typeId":["specId_1","specId_2"]}
     *
     * @param {Object} specs
     *
     * @return Object
     */
    _getForcedSpecsByType(specs){
        let types : idsObject = {ids:[]};

        for(let i = 0; i < specs.ids.length; i++){
            let itemId = specs.ids[i];
            let item = specs[itemId];
            let typeId = item.itemType;

            if(item.type === ItemAvailability.RECOMMENDED || item.type === ItemAvailability.REQUIRED){
                if(!types[typeId]){
                    types[typeId] = {
                        specs: []
                    };
                    types.ids.push(typeId);
                }
                types[typeId].specs.push(itemId);
            }
        }
        return types;
    }
    /**
     * Flattens the form data
     *
     * @param {Object} data
     */
    _flattenFormData(data){
        let flatDescItemGroups, flatGroups;

        if(data.itemTypes) {
            const refGroups = this.refTables.groups.data;
            const itemTypeMap = getMapFromList(data.itemTypes);
            const groups = refGroups.ids.map(id => {
                const group = refGroups[id];
                const types = group.itemTypes.map(it => {
                    const dataItemType = itemTypeMap[it.id] || {};
                    return {
                        ...it,
                        ...dataItemType
                    }
                });
                return {
                    code: group.code,
                    name: group.name,
                    types: types
                }
            });
            this._getTypesMap(groups);
        }
    }

    _getTypesMap(types){
        let specs, descItems;

        for(let t = 0; t < types.length; t++){
            let type = types[t];
            let typeDescItems = type.descItems;
            let typeSpecs = type.specs;

            if(typeSpecs && typeSpecs.length > 0){
                this._getSpecMap(typeSpecs, type);
            }

            if(typeDescItems && typeDescItems.length > 0){
                this._getDescItemsMap(typeDescItems, type);
            }

            this.types[type.id] = type;
            this.types.ids.push(type.id);
        }
    }

    _getSpecMap(specs, type){
        for(let s = 0; s < specs.length; s++){
            let spec = {
                ...specs[s],
                itemType: type.id
            };
            this.specs[spec.id] = spec;
            this.specs.ids.push(spec.id);
        }
    }

    _getDescItemsMap(items, type){
        for(let d = 0; d < items.length; d++){
            let item = {
                ...items[d],
                itemType: type.id
            };
            let itemId: string;

            if(item.objectId >= 0){
                itemId = item.objectId;
            } else {
                itemId = "item_" + this._emptyItemCounter;
                this._emptyItemCounter++;
            }

            if(itemId !== null){
                this.descItems[itemId] = item;
                this.descItems.ids.push(itemId);
            }
        }
    }
}

function replaceIdWithString(item, map){
    if(typeof item.type === "number"){
        item.type = map[item.type];
    }
    return item;
}

function replaceIdsWithString(items, map){
    for(let i = 0; i < items.ids.length; i++){
        let itemId = items.ids[i];
        items[itemId] = replaceIdWithString(items[itemId],map);
    }
    return items;
}

function merge(state : IItemFormState) {
    // Načten data map pro aktuální data, která jsou ve store - co klient zobrazuje (nemusí být, pokud se poprvé zobrazuje formulář)
    const dataMap = createDataMap(state.formData!!);

    const data = state.data!!;

    const idTypeToItemsMap = new Map<number, ApItemVO<any>[]>();
    data.items && data.items.forEach(item => {
        if (!idTypeToItemsMap.has(item.typeId!!)) {
            idTypeToItemsMap.set(item.typeId!!, []);
        }
        idTypeToItemsMap.get(item.typeId!!)!!.push(item);
    });


    // Mapa db id descItemType na descItemType
    const dbItemTypesMap = new Map<number, ItemTypeExt>();
    data.itemTypes.forEach(type => {
        if (idTypeToItemsMap.has(type.id) || type.type > 1) {
            const xtype = {
                ...type,
                items: (idTypeToItemsMap.get(type.id) as any as ApItemExt<any>[]) || []
            };
            dbItemTypesMap.set(type.id, xtype);
        }
    });

    // Procházíme všechny skupiny, které mohou být na formuláři - nikoli hodnoty z db, ty pouze připojujeme
    // Všechny procházíme z toho důvodu, že některé mohou být vynuceny na zobrazení - forceVisible a klient je musí zobrazit
    let itemTypes: ItemTypeExt[] = [];
    state.infoTypes!!.forEach(type => {
        // Merge descItemType

        const resultDescItemType : ItemTypeExt = {
            hasFocus: false,
            ...(dataMap.typeMap.get(type.id) || {}),   // připojení atributu již na klientovi, pokud existuje
            ...type,                            // přepsání novými daty ze serveru
            items: []
        } as any as ItemTypeExt;

        // Merge descItems
        // - DB verze
        // - původní verze descItem - data, která jsou aktuálně ve store
        const prevDescItemType = dataMap.typeMap.get(type.id);    // verze na klientovi, pokud existuje
        const newDescItemType = dbItemTypesMap.get(type.id);          // verze z db, pokud existuje

        if (mergeDescItems(state, resultDescItemType, prevDescItemType, newDescItemType)) {
            itemTypes.push(resultDescItemType);
        }
    });

    return {
        itemTypes
    };
}


// refTypesMap - mapa id info typu na typ, je doplněné o dataType objekt - obecný číselník
export function updateFormData(state : IItemFormState, data: ItemData, refTypesMap: Map<any, RefType>, dirty, updatedItem?: ApItemVO<any>) : IItemFormState {
    // Přechozí a nová verze node
    // const currentNodeVersionId = state.data ? state.data.parent.version : -1;
    // const newNodeVersionId = data.parent.version ? data.parent.version : currentNodeVersionId + 1; // TODO @compel chceme ?
    // ##
    // # Vytvoření formuláře se všemi povinnými a doporučenými položkami, které jsou doplněné reálnými daty ze serveru
    // # Případně promítnutí merge.
    // ##
    if (true || dirty) {// rovno musí být, protože i když mám danou verzi, nemusím mít nově přidané povinné položky (nastává i v případě umělého klientského zvednutí nodeVersionId po zápisové operaci) na základě aktuálně upravené mnou
        const newState = {
            ...state,
            infoTypesMap: new Map<number, RefTypeExt>(),// mapa id descItemTypeInfo na descItemTypeInfo
            unusedItemTypeIds: null,
            refTypesMap,// Mapa číselníku decsItemType
            data // Data přijatá ze serveru
        };
        // if(updatedItem){
        //     state.updatedItem = updatedItem;
        // }

        // Překopírování seznam id nepoužitých PP pro výstupy
        //state.unusedItemTypeIds = data.unusedItemTypeIds; // Tohle je potřeba ?

        // TODO: vyřešit problém s AP strukturovaný/nestrukturovaný
        // if (!data.itemTypes) {
        //     return state;
        // }

        const dataItemTypeMap = getMapFromList(data.itemTypes);

        // Info skupiny - ty, které jsou jako celek definované pro konkrétní JP - obsahují všechny atributy včetně např. typu - POSSIBLE atp.
        // Změna číselného typu na řetězec
        // Přidání do info skupin position
        newState.infoTypes = Object.values(data.itemTypes).map(it => {
            const itemType = refTypesMap.get(it.id)!!;
            const itemSpecs = itemType.descItemSpecsMap;

            const dataItemType = (dataItemTypeMap.get(itemType.id) || {}) as any as ItemTypeLiteVO;
            const dataItemSpecs = dataItemType.specs || [];

            const finalItemSpecs = Array.from(itemSpecs.values()).map(spec => {
                const specIndex = indexById(dataItemSpecs, spec.id);
                if (specIndex == null) {
                    return {
                        id: spec.id,
                        type: ItemAvailability.IMPOSSIBLE,
                        rep: 0
                    }
                } else {
                    const dataSpec = dataItemSpecs[specIndex];
                    return {
                        ...dataSpec,
                        type: ItemAvailabilityNumToEnumMap[dataSpec.type]
                    }
                }
            });

            const finalItemType = {
                ...dataItemType,
                type: (dataItemType.type ? ItemAvailabilityNumToEnumMap[dataItemType.type] : ItemAvailability.IMPOSSIBLE) as any as ItemAvailability,
                specs: finalItemSpecs,
                descItemSpecsMap: getMapFromList(finalItemSpecs),
            } as any as RefTypeExt;

            const resultItemType : RefTypeExt = {
                cal: 0,
                calSt: 0,
                descItemSpecsMap: {},
                favoriteSpecIds: [],
                id: itemType.id,
                ind: 0,
                rep: 0,
                specs: [],
                type: ItemAvailability.IMPOSSIBLE,
                width: 1,
                ...finalItemType
            };
            newState.infoTypesMap.set(resultItemType.id, resultItemType);
            return resultItemType;
        });

        // Mapa id descItemType na descItemType - existujících dat ze serveru
        //const dbItemTypesMap = getDbItemTypesMap(data)

        newState.formData = merge(newState);
        return newState;
    }
    return state;
}

export function createItem(descItemType: ItemTypeLiteVO, refType: RefType, addedByUser: boolean) {

    const result: ApItemExt<any> = {
        '@class': getItemClass(refType.dataType),
        typeId: refType.id,
        prevValue: null,
        hasFocus: false,
        touched: false,
        visited: false,
        saving: false,
        value: null,
        error: {hasError:false},
        addedByUser
    };

    initFormKey(descItemType, result);

    if (refType.useSpecification) {
        //result.specId = ''; // TODO specs
    }

    // Inicializační hodnoty pro nově vytvořenou položku
    switch (refType.dataType.code) {
        case "UNITDATE": // TODO Enum
            result.calendarTypeId = 1;
        break;
        case "JSON_TABLE":// TODO Enum
            result.value = { rows: [{ values: {} }] };
        break;
    }

    return result;
}

export function getItemClass(dataType) {
    switch (dataType.code) {
        case DataTypeCode.TEXT:
            return '.ApItemTextVO';
        case DataTypeCode.STRING:
            return '.ApItemStringVO';
        case DataTypeCode.INT:
            return '.ApItemIntVO';
        case DataTypeCode.COORDINATES:
            return '.ApItemCoordinatesVO';
        case DataTypeCode.DECIMAL:
            return '.ApItemDecimalVO';
        case DataTypeCode.PARTY_REF:
            return '.ApItemPartyRefVO';
        case DataTypeCode.FILE_REF:
            return '.ApItemFileRefVO';
        case DataTypeCode.RECORD_REF:
            return '.ApItemAccessPointRefVO';
        case DataTypeCode.STRUCTURED:
            return '.ApItemStructureVO';
        case DataTypeCode.JSON_TABLE:
            return '.ApItemJsonTableVO';
        case DataTypeCode.ENUM:
            return '.ApItemEnumVO';
        case DataTypeCode.FORMATTED_TEXT:
            return '.ApItemFormattedTextVO';
        case DataTypeCode.UNITDATE:
            return '.ApItemUnitdateVO';
        case DataTypeCode.UNITID:
            return '.ApItemUnitidVO';
        case DataTypeCode.DATE:
            return '.ApItemDateVO';
        case DataTypeCode.APFRAG_REF:
            return '.ApItemAPFragmentRefVO';
        default:
            console.error("Unsupported data type", dataType);
            return null;
    }
}
