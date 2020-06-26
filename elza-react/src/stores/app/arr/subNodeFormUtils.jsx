import {getMapFromList, indexById} from 'stores/app/utils.jsx';
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx';
import {DisplayType} from '../../../constants.tsx';
import {isNormalizeDurationLength, normalizeDurationLength, toDuration} from '../../../components/validate';
import {ItemAvailability, ItemAvailabilityNumToEnumMap} from "../accesspoint/itemFormUtils";
import {JAVA_ATTR_CLASS} from '../../../constants';

const availability = {
    REQUIRED: 'REQUIRED',
    RECOMMENDED: 'RECOMMENDED',
    POSSIBLE: 'POSSIBLE',
    IMPOSSIBLE: 'IMPOSSIBLE',
};

const typesNumToStrMap = {};
typesNumToStrMap[3] = availability.REQUIRED;
typesNumToStrMap[2] = availability.RECOMMENDED;
typesNumToStrMap[1] = availability.POSSIBLE;
typesNumToStrMap[0] = availability.IMPOSSIBLE;

function getDbItemTypesMap(data) {
    // Mapa id descItemType na descItemType
    var typesMap = {};
    data.groups.forEach(group => {
        group.types.forEach(type => {
            typesMap[type.id] = type;
        });
    });
    return typesMap;
}

/**
 * Vytvoření map na základě - descItemGroup - code, descItemType - id a descItem - descItemObjectId.
 * Mapa se vytváří na základě již existujícího formuláře, pokud existuje.
 * groupMap - mapa group.code na group
 * typeMap - mapa type.id na type
 * itemMap - mapa item.descItemObjectId na item
 */
function createDataMap(formData) {
    let groupMap = {};
    let typeMap = {};
    let itemMap = {};

    if (formData) {
        // nějaká data již existují
        formData.descItemGroups.forEach(group => {
            groupMap[group.code] = group;

            group.descItemTypes.forEach(type => {
                typeMap[type.id] = type;

                type.descItems.forEach(item => {
                    itemMap[item.descItemObjectId] = item;
                });
            });
        });
    }

    const get = (ths, key) => {
        return ths[key];
    };

    const find = (ths, key) => {
        return typeof ths[key] !== 'undefined' ? ths[key] : {};
    };

    groupMap.get = get.bind(null, groupMap);
    typeMap.get = get.bind(null, typeMap);
    itemMap.get = get.bind(null, itemMap);
    groupMap.find = find.bind(null, groupMap);
    typeMap.find = find.bind(null, typeMap);
    itemMap.find = find.bind(null, itemMap);

    return {
        groupMap,
        typeMap,
        itemMap,
    };
}

export function createImplicitDescItem(descItemType, refType, addedByUser = false) {
    let descItem = createDescItem(descItemType, refType, addedByUser);
    descItem.position = 1;
    return descItem;
}

export function getFocusDescItemLocation(subNodeFormStore) {
    const formData = subNodeFormStore.formData;

    for (let g = 0; g < formData.descItemGroups.length; g++) {
        const group = formData.descItemGroups[g];
        if (group.hasFocus) {
            for (let dit = 0; dit < group.descItemTypes.length; dit++) {
                const descItemType = group.descItemTypes[dit];
                if (descItemType.hasFocus) {
                    for (let di = 0; di < descItemType.descItems.length; di++) {
                        const descItem = descItemType.descItems[di];
                        if (descItem.hasFocus) {
                            return {
                                descItemGroupIndex: g,
                                descItemTypeIndex: dit,
                                descItemIndex: di,
                            };
                        }
                    }
                }
            }
        }
    }

    return null;
}

export function createDescItemFromDb(descItemType, descItem) {
    const result = {
        ...descItem,
        prevDescItemSpecId: descItem.descItemSpecId,
        prevValue: descItem.value,
        hasFocus: false,
        touched: false,
        visited: false,
        error: {hasError: false},
    };

    if (descItem.hasOwnProperty('description')) {
        result.prevDescription = descItem.description;
    }

    prepareNextFormKey(descItemType)
    initFormKey(descItemType, result);

    return result;
}

function prevDescItemHasSamePrevValue(prevDescItem, newDescItem) {
    return (
        prevDescItem.prevValue === newDescItem.value && prevDescItem.prevDescItemSpecId === newDescItem.descItemSpecId
    );
}

function addUid(descItem, index) {
    if (typeof descItem.descItemObjectId !== 'undefined') {
        descItem._uid = descItem.descItemObjectId;
    } else {
        descItem._uid = '_i' + index;
    }
}

var _formKeys = {};

export function prepareNextFormKey(descItemType) {
    _formKeys[descItemType.id] = _formKeys[descItemType.id] + 1
}


function initFormKey(descItemType, descItem) {
    if (descItem.formKey) {
        return;
    }

    if (!_formKeys[descItemType.id]) {
        _formKeys[descItemType.id] = 1;
    }
    descItem.formKey = 'fk_' + _formKeys[descItemType.id];
    // descItem.formKey = 'fk_' + descItemType.id;
    // console.log(1666666666666666666)
    // _formKeys[descItemType.id] = _formKeys[descItemType.id] + 1;
}

// 1. Doplní povinné a doporučené specifikace s prázdnou hodnotou, pokud je potřeba
// 2. Pokud atribut nemá žádnou hodnotu, přidá první implicitní
//
export function consolidateDescItems(resultDescItemType, infoType, refType, addedByUser, emptySystemSpecToKeyMap = {}) {
    var forceVisibility = infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED';

    // Vynucené hodnoty se specifikací, pokud je potřeba
    addForcedSpecifications(resultDescItemType, infoType, refType, emptySystemSpecToKeyMap);

    // Přidáme jednu hodnotu - chceme i u opakovatelného, pokud žádnou nemá (nebyla hodnota přifána vynucením specifikací)
    if (resultDescItemType.descItems.length === 0) {
        resultDescItemType.descItems.push(createImplicitDescItem(resultDescItemType, refType, addedByUser));
    }

    if (refType.dataType.code === 'INT' && refType.viewDefinition === DisplayType.DURATION) {
        resultDescItemType.descItems.forEach(descItem => {
            if (!isNaN(descItem.prevValue)) {
                descItem.prevValue = toDuration(descItem.prevValue);
            }
            if (!isNaN(descItem.value)) {
                descItem.value = toDuration(descItem.value);
            }
        });
    }

    resultDescItemType.descItems.forEach((descItem, index) => {
        descItem.position = index + 1;
    });
}

/**
 * Doplnění prázdných hodnot se specifikací, které jsou vynucené podle typu (REQUIRED a RECOMMENDED), pokud ještě v resultDescItemType nejsou.
 * Uvažujeme POUZE descItemType, které mají specifikaci a MAJÍ i hodnotu, né pouze specifikaci.
 */
export function addForcedSpecifications(resultDescItemType, infoType, refType, emptySystemSpecToKeyMap = {}) {
    if (!refType.useSpecification) {
        return;
    }

    if (!hasDescItemTypeValue(refType.dataType)) {
        return;
    }

    // Seznam existujících specifikací
    var existingSpecIds = {};
    resultDescItemType.descItems.forEach(descItem => {
        if (typeof descItem.descItemSpecId !== 'undefined' && descItem.descItemSpecId !== '') {
            existingSpecIds[descItem.descItemSpecId] = true;
        }
    });

    infoType.specs.forEach(spec => {
        const infoSpec = infoType.descItemSpecsMap[spec.id];
        var forceVisibility = infoSpec.type == 'REQUIRED' || infoSpec.type == 'RECOMMENDED';
        if (forceVisibility && !existingSpecIds[spec.id]) {
            // přidáme ji na formulář, pokud má být vidět a ještě na formuláři není
            var descItem = createImplicitDescItem(resultDescItemType, refType, false);
            descItem.descItemSpecId = spec.id;

            // Ponechání původního form key, pokud existovala tato položka již na klientovi a nebylo na ní šáhnuto
            var formKey = emptySystemSpecToKeyMap[spec.id];
            if (formKey) {
                descItem.formKey = formKey;
            }

            // U vícehodnotových přidáváme všechny, které neexistují, u jednohodnotového nesmí být více než jedna
            if (infoType.rep === 1) {
                // Vícehodnotový
                resultDescItemType.descItems.push(descItem);
            } else {
                // Jednohodnotový, přidáme jen jednu
                if (resultDescItemType.descItems.length === 0) {
                    // není žádná, přidáme první
                    resultDescItemType.descItems.push(descItem);
                }
            }
        }
    });
}

function mergeDescItems(state, resultDescItemType, prevType, newType) {
    var infoType = state.infoTypesMap[resultDescItemType.id];
    var refType = state.refTypesMap[resultDescItemType.id];
    var forceVisibility = infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED';

    if (!prevType) {
        // ještě ji na formuláři nemáme
        if (!newType) {
            // není ani v DB, přidáme ji pouze pokud je nastaveno forceVisibility
            if (forceVisibility) {
                // přidáme ji pouze pokud je nastaveno forceVisibility
                // Upravení a opravení seznamu hodnot, případně přidání rázdných
                consolidateDescItems(resultDescItemType, infoType, refType, false);

                return true;
            }
        } else {
            // je v db a není předchozí, dáme ji do formuláře bez merge
            newType.descItems.forEach(descItem => {
                resultDescItemType.descItems.push(createDescItemFromDb(resultDescItemType, descItem));
            });

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            consolidateDescItems(resultDescItemType, infoType, refType, false);

            return true;
        }
    } else {
        // již ji na formuláři máme, musíme provést merge
        if (!newType) {
            // není ani v DB, my jí máme, musíme nechat jen nově přidané hodnoty, protože ostatní i mnou editované již někdo smazal (protože nepřišel objekt newType)
            prevType.descItems.forEach(descItem => {
                if (typeof descItem.id === 'undefined' && descItem.addedByUser) {
                    // mnou přidaná ještě neuložená, necháme je
                    resultDescItemType.descItems.push(descItem);
                }
            });

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            if (forceVisibility) {
                const count = resultDescItemType.descItems.length;
                consolidateDescItems(resultDescItemType, infoType, refType, false);

                // Oprava incrementování formKey - nechceme zvýšit formKey v případě že se nic nezměnilo (předchozí položka není na serveru a je vynucená nová také)
                if (
                    count === 0 &&
                    resultDescItemType.descItems.length === 1 &&
                    prevType.descItems.length === 1 &&
                    prevType.descItems[0].formKey &&
                    !prevType.descItems[0].id
                ) {
                    resultDescItemType.descItems[0].formKey = prevType.descItems[0].formKey;
                }
            }

            // Chceme ji pokud má nějaké hodnoty
            if (resultDescItemType.descItems.length > 0) {
                return true;
            }
        } else {
            // je v db a my ji také máme, musíme provést merge
            // Vezmeme jako primární nově příchozí hodnoty a do nich přidáme ty, které aktualní klient má přidané, ale nemá je ještě uložené např. kvůli validaci atp.
            // Pokud ale má klient ty samé hodnoty (prev value je stejné jako nově příchozí hodnota), jako přijdou ze serveru a současně je upravil a nejsou uložené, necháme hodnoty v našem klientovi

            // Mapa existujících hodnot na klientovi
            var prevDescItemMap = {};
            prevType.descItems.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') {
                    // hodnota již dříve přijatá ze serveru
                    prevDescItemMap[descItem.descItemObjectId] = descItem;
                }
            });

            // Nakopírování nově přijatých hodnot, případně ponechání stejných (na základě descItemObjectId a prev value == value ze serveru, které již uživatel upravil a nejsou odeslané)
            newType.descItems.forEach(descItem => {
                var prevDescItem = prevDescItemMap[descItem.descItemObjectId];
                if (
                    prevDescItem &&
                    prevDescItemHasSamePrevValue(prevDescItem, descItem) &&
                    (prevDescItem.touched || (!descItem.value && !descItem.undefined))
                ) {
                    // původní hodnota přijatá ze serveru má stejné hodnoty jako jsou nyní v nově přijatých datech na serveru a uživatel nám aktuální data upravil
                    var item = prevDescItem;
                    if (state.updatedItem && state.updatedItem.descItemObjectId === descItem.descItemObjectId) {
                        item.value = state.updatedItem.value;
                    }
                    addUid(item, null);
                    item.formKey = prevDescItem.formKey;
                    resultDescItemType.descItems.push(item);
                } else {
                    var item = createDescItemFromDb(resultDescItemType, descItem);
                    addUid(item, null);
                    if (prevDescItem) {
                        item.formKey = prevDescItem.formKey;
                    } else {
                        prepareNextFormKey(resultDescItemType)
                        initFormKey(resultDescItemType, item);
                    }
                    resultDescItemType.descItems.push(item);
                }
            });

            // Doplnění o přidané a neuložené v aktuálním klientovi
            // Pokud se jedná o jednohodnotvý atribut, necháme jen tu ze serveru
            var emptySystemSpecToKeyMap = {}; // mapa id specifikace prázdné systémové vynucené položky specifikace na formKey dané položky - aby nám položky na formuláři neskákaly
            if (infoType.rep === 1) {
                // Vícehodnotový
                var prevDescItem = null;
                prevType.descItems.forEach((descItem, index) => {
                    addUid(descItem, index);

                    if (typeof descItem.id === 'undefined') {
                        // mnou přidaná ještě neuložená, musíme jí přidat na správné místo
                        // Pokud se jedná o systémově přidanou hodnotu a uživatel na ní zatím nešáhl, nebudeme ji vůbec uvažovat
                        if (!descItem.addedByUser && !descItem.touched) {
                            // systémově přidaná a neupravená
                            // nebudeme ji uvažovat, jen se pro ni budeme snažit zachovat formKey, aby nám položky na formuláři neskákaly - jedná se o systémnově přidané atributy s povinnou nebo doporučenou specifikací
                            if (refType.useSpecification && hasDescItemTypeValue(refType.dataType)) {
                                emptySystemSpecToKeyMap[descItem.descItemSpecId] = descItem.formKey;
                            }
                        } else {
                            if (prevDescItem) {
                                // má předchozí, zkusíme ji v novém rozložení dát na stejné místo, pokud to půjde
                                var index = indexById(resultDescItemType.descItems, prevDescItem._uid, '_uid');
                                if (index !== null) {
                                    // našli jsme položku, za kterou ji můžeme přidat
                                    resultDescItemType.descItems = [
                                        ...resultDescItemType.descItems.slice(0, index + 1),
                                        descItem,
                                        ...resultDescItemType.descItems.slice(index + 1),
                                    ];
                                } else {
                                    // nenašli jsme položku, za kterou ji můžeme přidat, dáme ji na konec
                                    resultDescItemType.descItems.push(descItem);
                                }
                            } else {
                                // nemá předchozí, dáme ji v novém rozložení na konec
                                resultDescItemType.descItems.push(descItem);
                            }
                        }
                    }

                    prevDescItem = descItem;
                });
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
function prepareFlatData(data) {
    // Již nahrazeno před posláním akce, zatím necháno, možná se bude celé refaktorovat
    // data.types = replaceIdsWithString(data.types, typesNumToStrMap);
    // data.specs = replaceIdsWithString(data.specs, typesNumToStrMap);
    data.types = insertDescItemSpecsMap(data.types, data.specs);
    return data;
}

/**
 * Adds item from response as descItem
 *
 * @param FlatFormData data
 * @param Object item
 */
function addChangedItemIfExists(data, item) {
    if (item) {
        if (!data.descItems) {
            data.descItems = {};
        }
        let itemId = item.descItemObjectId;
        data.descItems[itemId] = item;
        data.descItems.ids.push(itemId);
    }
}

/**
 * Gets descItem ids per type as map.
 *
 * @param Object items
 *
 * @return Object
 */
function getMapByItemType(items) {
    let types = {ids: []};

    for (let i = 0; i < items.ids.length; i++) {
        let itemId = items.ids[i];
        let item = items[itemId];
        let typeId = item.itemType;
        if (!types[typeId]) {
            types[typeId] = {
                items: [],
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
 * @param Object item
 * @param Object items
 *
 * @return Object
 */
function insertItemType(item, items) {
    item = {
        ...item,
        itemType: items[item.descItemObjectId].itemType,
    };
    return item;
}

export function mergeAfterUpdate(state, data, refTables) {
    // Hotfix pro Bug 4620 - Chyba při změně Složky na Jednotlivost
    // Potreba upravit ve funkci update ve FlatFormData
    data = fillImpossibleTypes(data, state.refTypesMap);

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

    console.log(8888, Object.values(flatLocalForm.types))
    console.log(9999999999999, flatForm)

    // Update info about descItemTypes
    // XXXXXXXX
    state.infoTypesMap = {};
    Object.keys(flatLocalForm.types).forEach(key => {
        const {descItems, ...type} = flatLocalForm.types[key];
        state.infoTypesMap[key] = type;
    })
    // state.infoTypesMap = flatLocalForm.types;

    console.log(8888, Object.values(flatLocalForm.types))

    // Update form with new data
    state.formData = restoreFormDataStructure(flatLocalForm, state.refTypesMap);

    // Odebrání pomocných dat - sice prasárna, ale jinak by se to muselo celé přepsat - commit 85921c4ed7d187d41759fa938370dcaac3da5aa1
    Object.values(flatLocalForm.types).forEach(type => {
        type.descItems && type.descItems.forEach(descItem => {
            delete descItem.itemType
        });
    });
    console.log(8888, Object.values(flatLocalForm.types))

    return state;
}

/**
 * Inserts map of specifications for descItems
 */
function insertDescItemSpecsMap(types, specs) {
    for (let s = 0; s < specs.ids.length; s++) {
        let specId = specs.ids[s];
        let spec = specs[specId];
        let type = types[spec.itemType];

        if (type) {
            if (!type.descItemSpecsMap) {
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
 * @param FlatFormData data
 *
 * @return Object
 * */
function restoreFormDataStructure(data, refTypesMap) {
    let groupId, group, typeId, type, descItemId, descItem, specId, spec;
    let usedTypes = {ids: []};
    let usedGroups = {ids: []};
    let descItemGroups = [];

    for (let d = 0; d < data.descItems.ids.length; d++) {
        descItemId = data.descItems.ids[d];
        descItem = data.descItems[descItemId];

        if (!usedTypes[descItem.itemType]) {
            type = data.types[descItem.itemType];
            type.descItems = [];
            type.specs = [];
            usedTypes[descItem.itemType] = type;
            usedTypes.ids.push(descItem.itemType);
        }

        const refType = refTypesMap[type.id];
        if (refType.dataType.code === 'INT' && refType.viewDefinition === DisplayType.DURATION) {
            if (!isNaN(descItem.prevValue)) {
                descItem.prevValue = toDuration(descItem.prevValue);
            } else if (!isNormalizeDurationLength(descItem.prevValue)) {
                descItem.prevValue = normalizeDurationLength(descItem.prevValue);
            }
            if (!isNaN(descItem.value)) {
                descItem.value = toDuration(descItem.value);
            } else if (!isNormalizeDurationLength(descItem.value)) {
                descItem.value = normalizeDurationLength(descItem.value);
            }
        }

        usedTypes[descItem.itemType].descItems.push(descItem);
    }

    for (let s = 0; s < data.specs.ids.length; s++) {
        specId = data.specs.ids[s];
        spec = data.specs[specId];

        if (usedTypes[spec.itemType]) {
            usedTypes[spec.itemType].specs.push(spec);
        }
    }

    for (let t = 0; t < usedTypes.ids.length; t++) {
        typeId = usedTypes.ids[t];
        type = usedTypes[typeId];

        if (!usedGroups[type.group]) {
            group = data.groups[type.group];
            group.descItemTypes = [];
            usedGroups[type.group] = group;
            usedGroups.ids.push(type.group);
        }
        usedGroups[type.group].descItemTypes.push(type);
    }

    for (let g = 0; g < usedGroups.ids.length; g++) {
        groupId = usedGroups.ids[g];
        group = usedGroups[groupId];
        descItemGroups.push(group);
    }

    return {
        descItemGroups,
    };
}

class FlatFormData {
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

    constructor(refTables) {
        this._emptyItemCounter = 0;
        this.groups = {ids: []};
        this.types = {ids: []};
        this.descItems = {ids: []};
        this.specs = {ids: []};
        this.refTables = refTables;
    }

    /**
     * Loads form data
     *
     * @param data
     */
    init(data) {
        this.groups = data.groups;
        this.types = data.types;
        this.descItems = data.descItems;
        this.specs = data.specs;
    }

    /**
     * Flattens and loads form data
     *
     * @param data
     */
    flattenInit(data) {
        this._flattenFormData(data);
    }

    /**
     * Updates current form data with given form data
     *
     * @param Object newData
     */
    update(newData) {
        this.groups = newData.groups;
        this.types = newData.types;
        this.specs = newData.specs;

        this._updateDescItems(newData.descItems);
    }

    /**
     * Updates descItems with given items.
     *
     * @param Object newItems
     */
    _updateDescItems(newItems) {
        this._mergeDescItems(newItems);
        this._deleteUnusedItems();
        this._generateNewItems();
    }

    /**
     * Merges given items into instance's descItems.
     * Modifies this.descItems.
     *
     * @param Object newItems
     */
    _mergeDescItems(newItems) {
        let items = this.descItems;
        let types = this.types;

        for (let i = 0; i < newItems.ids.length; i++) {
            let newItemId = newItems.ids[i];
            let item = items[newItemId];
            let newItem = newItems[newItemId];

            if (!item) {
                items.ids.push(newItem.descItemObjectId);
            }
            if (item.prevValue !== newItem.value) {
                newItem.value = item.value;
            }
            newItem = createDescItemFromDb(types[newItem.itemType], newItem);
            items[newItemId] = newItem;
        }
        this.descItems = items;
    }

    /**
     * Deletes unused items (items that are empty, and not added by user).
     */
    _deleteUnusedItems() {
        let items = this.descItems;
        let newIds = [...items.ids];

        for (let i = 0; i < items.ids.length; i++) {
            const itemId = items.ids[i];
            const item = items[itemId];
            const isEmpty = item.value === null || item.descItemSpecId === null;
            const type = this.types[item.itemType];
            const isFromDb = item.descItemObjectId >= 0;

            if (isEmpty && !item.addedByUser && !isFromDb) {
                delete items[itemId];
                newIds.splice(newIds.indexOf(itemId), 1);
            }
        }
        items.ids = newIds;
        this.descItems = items;
    }

    /**
     * Generates new object with desc items. Adds REQUIRED and RECOMMENDED item types and specifications.
     */
    _generateNewItems() {
        let types = this.types;
        let items = this.descItems;
        let specs = this.specs;
        let itemTypesMap = getMapByItemType(items);
        let itemSpecsMap = this._getForcedSpecsByType(specs);
        let refTypesMap = getMapFromList(this.refTables.descItemTypes.items);
        let refDataTypesMap = getMapFromList(this.refTables.rulDataTypes.items);
        let newItems = {ids: []};

        for (let t = 0; t < types.ids.length; t++) {
            let typeId = types.ids[t];
            let type = types[typeId];
            let forceVisible = type.type === availability.REQUIRED || type.type === availability.RECOMMENDED;
            let typeItems = itemTypesMap[typeId] && itemTypesMap[typeId].items;

            if (forceVisible) {
                let refType = refTypesMap[typeId];
                refType.dataType = refDataTypesMap[refType.dataTypeId];
                let newItem;
                let nextEmptyItemIdBase = 'item_';
                let nextEmptyItemId = nextEmptyItemIdBase + this._emptyItemCounter;
                let forcedTypeSpecs = itemSpecsMap[typeId] && itemSpecsMap[typeId].specs;

                //Add forced specifications
                // Do not force enum values -> has to be entered manually by user
                if (forcedTypeSpecs && refType.dataType.code !== 'ENUM') {
                    let lastPosition = typeItems ? typeItems.length : 0;
                    let unusedForcedSpecs = this._getUnusedSpecIds(forcedTypeSpecs, typeItems);

                    for (let s = 0; s < unusedForcedSpecs.length; s++) {
                        nextEmptyItemId = nextEmptyItemIdBase + this._emptyItemCounter;
                        newItem = createDescItem(type, refType, false);
                        newItem.descItemSpecId = unusedForcedSpecs[s];
                        newItem.itemType = typeId;
                        newItem.position = lastPosition + 1;
                        lastPosition++;

                        newItems[nextEmptyItemId] = newItem;
                        newItems.ids.push(nextEmptyItemId);
                        this._emptyItemCounter++;
                    }
                }
                //Add forced itemTypes
                else if (!typeItems) {
                    newItem = createDescItem(type, refType, false);
                    newItem.position = 1;
                    newItem.itemType = typeId;

                    newItems[nextEmptyItemId] = newItem;
                    newItems.ids.push(nextEmptyItemId);
                    this._emptyItemCounter++;
                }
            }
            if (typeItems) {
                //Add existing items
                for (let i = 0; i < typeItems.length; i++) {
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
     * @param Array specs - Array of spec ids
     * @param Array items - Array of descItem ids
     *
     * @return Array
     */
    _getUnusedSpecIds(specIds = this.specs.ids, itemIds = this.descItems.ids) {
        let unusedSpecIds = [...specIds];
        for (let i = 0; i < itemIds.length; i++) {
            let itemId = itemIds[i];
            let item = this.descItems[itemId];
            let specIndex = unusedSpecIds.indexOf(item.descItemSpecId);
            if (specIndex >= 0) {
                unusedSpecIds.splice(specIndex, 1);
            }
        }
        return unusedSpecIds;
    }

    /**
     * Returns Object of required or recommended spec ids (in array), mapped to item type ids
     * Ex.: {"typeId":["specId_1","specId_2"]}
     *
     * @param Object specs
     *
     * @return Object
     */
    _getForcedSpecsByType(specs) {
        let types = {ids: []};

        for (let i = 0; i < specs.ids.length; i++) {
            let itemId = specs.ids[i];
            let item = specs[itemId];
            let typeId = item.itemType;

            if (item.type === availability.RECOMMENDED || item.type === availability.REQUIRED) {
                if (!types[typeId]) {
                    types[typeId] = {
                        specs: [],
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
     * @param Object data
     */
    _flattenFormData(data) {
        let flatDescItemGroups, flatGroups;

        if (data.descItemGroups) {
            this._getGroupsMap(data.descItemGroups);
        }
        if (data.itemTypes) {
            const refGroups = this.refTables.groups.data;
            const itemTypeMap = getMapFromList(data.itemTypes);
            const groups = refGroups.ids.map((id,index) => {
                const group = refGroups[id];
                const types = group.itemTypes.map(it => {
                    const dataItemType = itemTypeMap[it.id] || {};
                    return {
                        ...it,
                        ...dataItemType,
                        // xxxxxx: it,
                        hasFocus: false,
                        group: group.code,
                    };
                });
                return {
                    code: group.code,
                    name: group.name,
                    types: types,
                    position: index + 1,
                    hasFocus: false,
                };
            });
            this._getGroupsMap(groups);
        }

        if (data.typeGroups) {
            this._getGroupsMap(data.typeGroups);
        }
    }

    _getGroupsMap(groups) {
        let flatTypes, flatDescItemTypes, newDescItems;

        for (let g = 0; g < groups.length; g++) {
            let group = groups[g];

            if (group.descItemTypes && group.descItemTypes.length > 0) {
                this._getTypesMap(group.descItemTypes, group);
            }

            if (group.types && group.types.length > 0) {
                this._getTypesMap(group.types, group);
            }

            this.groups[group.code] = group;
            this.groups.ids.push(group.code);
        }
    }

    _getTypesMap(types, group) {
        let specs, descItems;

        for (let t = 0; t < types.length; t++) {
            let type = {
                ...types[t],
                group: group.code,
            };
            let typeDescItems = type.descItems;
            let typeSpecs = type.specs;

            if (typeSpecs && typeSpecs.length > 0) {
                this._getSpecMap(typeSpecs, type);
            }

            if (typeDescItems && typeDescItems.length > 0) {
                this._getDescItemsMap(typeDescItems, type);
            }

            this.types[type.id] = type;
            this.types.ids.push(type.id);
        }
    }

    _getSpecMap(specs, type) {
        for (let s = 0; s < specs.length; s++) {
            let spec = {
                ...specs[s],
                itemType: type.id,
            };
            this.specs[spec.id] = spec;
            this.specs.ids.push(spec.id);
        }
    }

    _getDescItemsMap(items, type) {
        for (let d = 0; d < items.length; d++) {
            let item = {
                ...items[d],
                // AAAAAAAAAAAAAAAAAAAAAAAAA
                itemType: type.id,
                // aaaaaaaa: 11111111
            };
            let itemId = null;

            if (item.descItemObjectId >= 0) {
                itemId = item.descItemObjectId;
            } else {
                itemId = 'item_' + this._emptyItemCounter;
                this._emptyItemCounter++;
            }

            if (itemId !== null) {
                this.descItems[itemId] = item;
                this.descItems.ids.push(itemId);
            }
        }
    }
}

function replaceIdWithString(item, map) {
    if (typeof item.type === 'number') {
        item.type = map[item.type];
    }
    return item;
}

function replaceIdsWithString(items, map) {
    for (let i = 0; i < items.ids.length; i++) {
        let itemId = items.ids[i];
        items[itemId] = replaceIdWithString(items[itemId], map);
    }
    return items;
}

function merge(state) {
    // Načten data map pro aktuální data, která jsou ve store - co klient zobrazuje (nemusí být, pokud se poprvé zobrazuje formulář)
    const dataMap = createDataMap(state.formData);

    let descItemsByType = {};
    state.data.descItems &&
        state.data.descItems.forEach(item => {
            let items = descItemsByType[item.itemTypeId];
            if (!items) {
                items = [];
                descItemsByType[item.itemTypeId] = items;
            }
            items.push({
                ...item,
                // zzzzzzzzzzzzzzzzzzzzzzzz
                // itemType: item.itemTypeId
            });
        });

    // Mapa db id descItemType na descItemType
    let dbItemTypesMap = {};
    state.data.itemTypes.forEach(type => {
        if (descItemsByType[type.id] || type.type > 1) {
            const xtype = {
                ...type,
                descItems: descItemsByType[type.id] || [],
            };
            dbItemTypesMap[type.id] = xtype;
        }
    });

    // Procházíme všechny skupiny, které mohou být na formuláři - nikoli hodnoty z db, ty pouze připojujeme
    // Všechny procházíme z toho důvodu, že některé mohou být vynuceny na zobrazení - forceVisible a klient je musí zobrazit
    let descItemGroups = [];
    state.infoGroups.forEach(group => {
        const resultGroup = {
            hasFocus: false,
            ...dataMap.groupMap.find(group.code), // připojení skupiny již na klientovi, pokud existuje
            ...group, // přepsání novými daty ze serveru
            descItemTypes: [],
        };

        // Merge descItemType
        group.types.forEach(descItemType => {
            const resultDescItemType = {
                hasFocus: false,
                ...dataMap.typeMap.find(descItemType.id), // připojení atributu již na klientovi, pokud existuje
                ...descItemType, // přepsání novými daty ze serveru
                descItems: [],
            };

            // Merge descItems
            // - DB verze
            // - původní verze descItem - data, která jsou aktuálně ve store
            const prevDescItemType = dataMap.typeMap.get(descItemType.id); // verze na klientovi, pokud existuje
            const newDescItemType = dbItemTypesMap[descItemType.id]; // verze z db, pokud existuje

            if (mergeDescItems(state, resultDescItemType, prevDescItemType, newDescItemType)) {
                resultGroup.descItemTypes.push(resultDescItemType);
            }
        });

        if (resultGroup.descItemTypes.length > 0) {
            // skupinu budeme uvádět pouze pokud má nějaké atributy k zobrazení (povinné nebo doporučené)
            descItemGroups.push(resultGroup);
        }
    });

    return {
        descItemGroups: descItemGroups,
    };
}

/**
 * Doplní v přijatých datech ze serveru nemožné typy atributů.
 *
 * @param data data přijatá ze serveru
 * @param refTypesMap
 * @returns {*}
 */
function fillImpossibleTypes(data, refTypesMap) {
    const dataItemTypeMap = getMapFromList(data.itemTypes);

    Object.keys(refTypesMap).forEach(itemTypeId => {
        const itemTypeFound = indexById(data.itemTypes, itemTypeId);
        if (itemTypeFound == null) {
            const itemType = refTypesMap[itemTypeId];
            const itemSpecs = itemType.descItemSpecs;

            const dataItemType = dataItemTypeMap[itemTypeId] || {};
            const dataItemSpecs = dataItemType.specs || [];

            const finalItemSpecs = itemSpecs.map(spec => {
                const specIndex = indexById(dataItemSpecs, spec.id);
                if (specIndex == null) {
                    return {
                        id: spec.id,
                        type: ItemAvailabilityNumToEnumMap[0],
                        rep: 0,
                        itemType: itemType.id
                    };
                } else {
                    return {
                        ...dataItemSpecs[specIndex],
                        itemType: itemType.id
                    };
                }
            });

            const finalItemType = {
                ...dataItemType,
                specs: finalItemSpecs,
                descItemSpecsMap: getMapFromList(finalItemSpecs),
            };
            const resultItemType = {
                id: itemType.id,
                type: ItemAvailabilityNumToEnumMap[0],
                rep: 0,
                cal: 0,
                calSt: 0,
                favoriteSpecIds: [],
                ind: 0,
                specs: [],
                width: 1,
                ...finalItemType,
            };
            data.itemTypes.push(resultItemType);
        }
    });

    return data;
}

// refTypesMap - mapa id info typu na typ, je doplněné o dataType objekt - obecný číselník
export function updateFormData(state, data, refTypesMap, groups, updatedItem, dirty) {
    // Přechozí a nová verze node
    const currentNodeVersionId = state.data ? state.data.parent.version : -1;
    const newNodeVersionId = data.parent.version;
    // ##
    // # Vytvoření formuláře se všemi povinnými a doporučenými položkami, které jsou doplněné reálnými daty ze serveru
    // # Případně promítnutí merge.
    // ##
    if (currentNodeVersionId <= newNodeVersionId || dirty) {
        // rovno musí být, protože i když mám danou verzi, nemusím mít nově přidané povinné položky (nastává i v případě umělého klientského zvednutí nodeVersionId po zápisové operaci) na základě aktuálně upravené mnou
        // Data přijatá ze serveru
        state.data = fillImpossibleTypes(data, refTypesMap);

        if (updatedItem) {
            state.updatedItem = updatedItem;
        }

        // Překopírování seznam id nepoužitých PP pro výstupy
        state.unusedItemTypeIds = data.unusedItemTypeIds;

        const dataItemTypeMap = getMapFromList(data.itemTypes);

        // Info skupiny - ty, které jsou jako celek definované pro konkrétní JP - obsahují všechny atributy včetně např. typu - POSSIBLE atp.
        // Změna číselného typu na řetězec
        // Přidání do info skupin position
        state.infoGroupsMap = {};
        state.infoTypesMap = {}; // mapa id descItemTypeInfo na descItemTypeInfo

        state.infoGroups = !groups
            ? []
            : groups.ids.map((groupId, index) => {
                  const group = groups[groupId];
                  const resultGroup = {
                      code: group.code,
                      name: group.name,
                      position: index + 1,
                      types: group.itemTypes.map(it => {
                          const itemType = refTypesMap[it.id];
                          const itemSpecs = itemType.descItemSpecs;

                          const dataItemType = dataItemTypeMap[it.id] || {};
                          const dataItemSpecs = dataItemType.specs || [];

                          const finalItemSpecs = itemSpecs.map(spec => {
                              const specIndex = indexById(dataItemSpecs, spec.id);
                              if (specIndex == null) {
                                  return {
                                      id: spec.id,
                                      type: ItemAvailability.IMPOSSIBLE,
                                      rep: 0,
                                      itemType: it.id,
                                  };
                              } else {
                                  const dataSpec = dataItemSpecs[specIndex];
                                  return {
                                      ...dataSpec,
                                      itemType: it.id,
                                      // type: typesNumToStrMap[dataSpec.type],
                                  };
                              }
                          });

                          const finalItemType = {
                              hasFocus: false,
                              ...dataItemType,
                              type: dataItemType.type ? dataItemType.type : ItemAvailability.IMPOSSIBLE,
                              // type: dataItemType.type ? typesNumToStrMap[dataItemType.type] : ItemAvailability.IMPOSSIBLE,
                              specs: finalItemSpecs,
                              descItemSpecsMap: getMapFromList(finalItemSpecs),
                          };

                          const resultItemType = {
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
                              group: group.code,
                              ...finalItemType,
                          };
                          state.infoTypesMap[resultItemType.id] = resultItemType;

                          return resultItemType;
                      }),
                  };

                  state.infoGroupsMap[resultGroup.code] = resultGroup;

                  return resultGroup;
              });

        // Mapa číselníku decsItemType
        state.refTypesMap = refTypesMap;

        // Mapa id descItemType na descItemType - existujících dat ze serveru
        //var dbItemTypesMap = getDbItemTypesMap(data)

        const newFormData = merge(state);
        state.formData = newFormData;
    }
}

export function createDescItem(descItemType, refType, addedByUser) {
    var result = {
        [JAVA_ATTR_CLASS]: getItemClass(refType.dataType),
        prevValue: null,
        hasFocus: false,
        touched: false,
        visited: false,
        saving: false,
        value: null,
        error: {hasError: false},
        addedByUser,
    };

    initFormKey(descItemType, result);

    if (refType.useSpecification) {
        result.descItemSpecId = '';
    }

    // Inicializační hodnoty pro nově vytvořenou položku
    switch (refType.dataType.code) {
        case 'UNITDATE':
            result.calendarTypeId = 1;
            break;
        case 'JSON_TABLE':
            result.value = {rows: [{values: {}}]};
            break;
        default:
            break;
    }

    return result;
}

export function getItemClass(dataType) {
    switch (dataType.code) {
        case 'TEXT':
            return '.ArrItemTextVO';
        case 'STRING':
            return '.ArrItemStringVO';
        case 'INT':
            return '.ArrItemIntVO';
        case 'COORDINATES':
            return '.ArrItemCoordinatesVO';
        case 'DECIMAL':
            return '.ArrItemDecimalVO';
        case 'FILE_REF':
            return '.ArrItemFileRefVO';
        case 'RECORD_REF':
            return '.ArrItemRecordRefVO';
        case 'STRUCTURED':
            return '.ArrItemStructureVO';
        case 'JSON_TABLE':
            return '.ArrItemJsonTableVO';
        case 'ENUM':
            return '.ArrItemEnumVO';
        case 'FORMATTED_TEXT':
            return '.ArrItemFormattedTextVO';
        case 'UNITDATE':
            return '.ArrItemUnitdateVO';
        case 'UNITID':
            return '.ArrItemUnitidVO';
        case 'DATE':
            return '.ArrItemDateVO';
        default:
            console.error('Unsupported data type', dataType);
            return null;
    }
}

const hasString = (str) => {
    return str != null && typeof str === 'string' && str.length > 0;
}

const isBool = (b) => {
    return b != null && typeof b === 'boolean';
}

const isNumber = (number) => {
    return number != null && typeof number === 'number';
}

const notNull = (obj) => {
    return obj != null;
}

function log(field, data, fce) {
    const value = data[field];
    const result = fce(value);
    if (!result) {
        console.warn(field + ' invalid: ' + value, data);
    }
}

function checkDescItemSpec(spec) {
    log('itemType', spec, isNumber);
}

function checkDescItem(item) {
    log('itemType', item, isNumber);
}

function checkDescItemType(type) {
    log('cal', type, isNumber);
    log('calSt', type, isNumber);
    log('width', type, isNumber);
    log('ind', type, isNumber);
    log('rep', type, isNumber);
    log('hasFocus', type, isBool);
    log('type', type, hasString);
    log('group', type, hasString);
    log('descItemSpecsMap', type, notNull);

    const descItemSpecsMap = type.descItemSpecsMap;
    if (descItemSpecsMap != null) {
        Object.keys(descItemSpecsMap).forEach(key => {
            checkDescItemSpec(descItemSpecsMap[key]);
        });
    }

    const descItems = type.descItems;
    if (descItems != null) {
        descItems.forEach(descItem => {
            checkDescItem(descItem);
        });
    }
}

function checkSpec(spec) {
    log('id', spec, isNumber);
    log('itemType', spec, isNumber);
    log('rep', spec, isNumber);
    log('type', spec, hasString);
}

function checkType(type) {
    log('descItemSpecsMap', type, notNull);
    log('group', type, hasString);
    const specs = type.specs;
    if (specs != null) {
        specs.forEach(spec => {
            checkSpec(spec);
        });
    }
}

function checkGroup(group) {
    log('code', group, hasString);
    log('name', group, hasString);
    log('position', group, isNumber);
    log('hasFocus', group, isBool);

    const descItemTypes = group.descItemTypes;
    descItemTypes.forEach(type => {
        checkDescItemType(type);
    });

    const types = group.types;
    types.forEach(type => {
        checkType(type);
    });
}

/**
 * Kontrola struktury store - zda-li jsou vyplněná požadovaná data.
 * - metoda je pouze pro ladící účely
 */
export function checkFormData(formData = {}, msg = "#checkFormData") {
    return;
    // POUZE PRO TESTOVACÍ ÚČELY
    if (formData) {
        const descItemGroups = formData.descItemGroups;
        console.log(msg, descItemGroups);
        descItemGroups.forEach(descItemGroup => {
            checkGroup(descItemGroup);
        });
    }
}
