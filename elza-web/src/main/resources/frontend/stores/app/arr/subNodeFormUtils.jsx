import {indexById} from 'stores/app/utils.jsx'

function getDescItemTypesMap(data) {
    // Mapa id descItemType na descItemType
    var descItemTypesMap = {};
    data.descItemGroups.forEach(group => {
        group.descItemTypes.forEach(descItemType => {
            descItemTypesMap[descItemType.id] = descItemType;
        })
    })
    return descItemTypesMap;
}

function getDescItemTypeGroupsInfo(data, rulDataTypes) {
    // Seznam všech atributů - obecně, doplněný o rulDataType
    // Doplnění position ke skupině
    var descItemTypeInfos = [];
    var descItemTypeGroupsMap = {};
    data.descItemTypeGroups.forEach((descItemGroup, descItemGroupIndex) => {
        descItemTypeGroupsMap[descItemGroup.code] = descItemGroup;
        descItemGroup.position = descItemGroupIndex;
        descItemGroup.descItemTypes.forEach(descItemType => {
            var rulDataType = rulDataTypes.items[indexById(rulDataTypes.items, descItemType.dataTypeId)];

            var descItemTypeInfo = Object.assign({}, descItemType, { descItemGroup: descItemGroup, rulDataType: rulDataType});
            descItemTypeInfos.push(descItemTypeInfo);
        });
    });

    return {
        descItemTypeInfos,
        descItemTypeGroupsMap,
    }
}

/**
 * Vytvoření map na základě - descItemGroup - code, descItemType - id a descItem - descItemObjectId.
 * Mapa se vytváří na základě již existujícího formuláře, pokud existuje.
 */
function createDataMap(formData) {
    var groupMap = {}
    var typeMap = {}
    var itemMap = {}

    if (formData) { // nějaká data již existují
        formData.descItemGroups.forEach(group => {
            groupMap[group.code] = group;
            
            group.descItemTypes.forEach(type => {
                typeMap[type.id] = type;

                type.descItems.forEach(item => {
                    itemMap[item.descItemObjectId] = item;
                })
            })
        })
    }

    var get = (ths, key) => {
        return ths[key]
    }

    var find = (ths, key) => {
        return typeof ths[key] !== 'undefined' ? ths[key] : {}
    }

    groupMap.get = get.bind(null, groupMap)
    typeMap.get = get.bind(null, typeMap)
    itemMap.get = get.bind(null, itemMap)
    groupMap.find = find.bind(null, groupMap)
    typeMap.find = find.bind(null, typeMap)
    itemMap.find = find.bind(null, itemMap)

    return {
        groupMap,
        typeMap,
        itemMap,
    }
}

function createImplicitDescItem(descItemType, descItemTypeInfos) {
    var descItemTypeInfo = descItemTypeInfos[indexById(descItemTypeInfos, descItemType.id)];                        
    var descItem = createDescItem(descItemTypeInfo, false);
    descItem.position = 1;
    return descItem;
}

export function createDescItemFromDb(descItem) {
    return {
        ...descItem,
        prevDescItemSpecId: descItem.descItemSpecId,
        prevValue: descItem.value,
        hasFocus: false,
        touched: false,
        visited: false,
        error: {hasError:false}
    }
}

function prevDescItemHasSamePrevValue(prevDescItem, newDescItem) {
    return prevDescItem.prevValue === newDescItem.value && prevDescItem.prevDescItemSpecId === newDescItem.descItemSpecId
}

function addUid(descItem, index) {
    if (typeof descItem.descItemObjectId !== 'undefined') {
        descItem._uid = descItem.descItemObjectId;
    } else {
        descItem._uid = "_i" + index;
    }
}

function mergeDescItems(resultDescItemType, prevType, newType, descItemTypeInfos) {
    var forceVisibility = resultDescItemType.type == 'REQUIRED' || resultDescItemType.type == 'RECOMMENDED'

    if (!prevType) {    // ještě ji na formuláři nemáme
        if (!newType) { // není ani v DB, přidáme ji pouze pokud je nastaveno forceVisibility
            if (forceVisibility) {  // přidáme ji pouze pokud je nastaveno forceVisibility
                if (resultDescItemType.repeatable) {    // u opakovatelného nepřidávíme implicitně prázdnou hodnotu, nemá to smysl
                    // Nic není třeba dělat
                } else {
                    // Přidáme jednu hodnotu - jedná se o jednohodnotový atribut
                    resultDescItemType.descItems.push(createImplicitDescItem(resultDescItemType, descItemTypeInfos));
                }
                return true;
            } else {
                return false;
            }
        } else {    // je v db a není předchozí, dáme ji do formuláře bez merge
            newType.descItems.forEach(descItem => {
                resultDescItemType.descItems.push(createDescItemFromDb(descItem))
            })
            return true;
        }
    } else {    // již ji na formuláři máme, musíme provést merge
        if (!newType) { // není ani v DB, my jí máme, musíme nechat jen nově přidané hodnoty, protože ostatní i mnou editované již někdo smazal (protože nepřišel objekt newType)
            prevType.descItems.forEach(descItem => {
                if (typeof descItem.id === 'undefined' && descItem.addedByUser) { // mnou přidaná ještě neuložená, necháme ji
                    resultDescItemType.descItems.push(descItem);
                }
            })

            if (forceVisibility && !resultDescItemType.repeatable && resultDescItemType.descItems.length === 0) { // má být vidět, je jednohodnotový ale nemáme žádnou hodnotu, přidáme implcitiní prázdnou
                // Přidáme jednu hodnotu - jedná se o jednohodnotový atribut
                resultDescItemType.descItems.push(createImplicitDescItem(resultDescItemType, descItemTypeInfos));
            }

            // Chceme ji pokud, má nějaké hodnoty nebo je vícehodnotová - ještě ale uživatel žádnou hodnotu nepřidal, nebo pokud má být vidět - forceVisibility
            return resultDescItemType.descItems.length > 0 || resultDescItemType.repeatable || forceVisibility;
        } else {    // je v db a my ji také máme, musíme provést merge
            // Vezmeme jako primární nově příchozí hodnoty a do nich přidáme ty, které aktualní klient má přidané, ale nemá je ještě uložené např. kvůli validaci atp.
            // Pokud ale má klient ty samé hodnoty (prev value je stejné jako nově příchozí hodnota), jako přijdou ze serveru a současně je upravil a nejsou uložené, necháme hodnoty v našem klientovi
            
            // Mapa existujících hodnot na klientovi
            var prevDescItemMap = {}
            prevType.descItems.forEach(descItem => {
                if (typeof descItem.id !== 'undefined') { // hodnota již dříve přijatá ze serveru
                    prevDescItemMap[descItem.descItemObjectId] = descItem;
                }
            })

            // Nakopírování nově přijatých hodnot, případně ponechání stejných (na základe descItemObjectId a prev value == value ze serveru, které již uživatel upravil a nejsou odeslané)
            newType.descItems.forEach(descItem => {
                var prevDescItem = prevDescItemMap[descItem.descItemObjectId];

                if (prevDescItem && prevDescItemHasSamePrevValue(prevDescItem, descItem) && prevDescItem.touched) {   // původní hodnota přijatá ze serveru má stejné hodnoty jako jsou nyní v nově přijatých datech na serveru a uživatel nám aktuální data upravil
                    var item = prevDescItem;
                    addUid(item, null);
                    resultDescItemType.descItems.push(item)
                } else {
                    var item = createDescItemFromDb(descItem);
                    addUid(item, null);
                    resultDescItemType.descItems.push(item)
                }
            })

            // Doplnění o přidané a neuložené v aktuálním klientovi, pouze pokud se jedná o vícehodnotový atribut - nedoplňujeme poud přišla ze serveru hodnota
            if (resultDescItemType.repeatable || (!resultDescItemType.repeatable && resultDescItemType.descItems.length == 0)) {
                var prevDescItem = null;
                prevType.descItems.forEach((descItem, index) => {
                    addUid(descItem, index);

                    if (typeof descItem.id === 'undefined') { // mnou přidaná ještě neuložená, musíme jí přidat a správné místo
                        if (prevDescItem) { // má předchozí, zkusíme ji v novém rozložní dat na stejné místo, pokud to půjde
                            var index = indexById(resultDescItemType.descItems, prevDescItem._uid, '_uid')
                            if (index !== null) {   // našli jsme položku, za kterou ji můžeme přidat
                                resultDescItemType.descItems = [
                                    ...resultDescItemType.descItems.slice(0, index + 1),
                                    descItem,
                                    ...resultDescItemType.descItems.slice(index + 1)
                                ]
                            } else {    // nenašli jsme položku, za kterou ji můžeme přidat, dáme ji na konec
                                resultDescItemType.descItems.push(descItem);
                            }
                        } else {    // nemá předchozí, dáme ji v novém rozložení na konec
                            resultDescItemType.descItems.push(descItem);
                        }
                    }

                    prevDescItem = descItem;
                })  
            }

            return true;
        }
    }

    return false;
}

function merge(formData, data, descItemTypesMap, rulDataTypes, descItemTypeInfos) {
    var dataMap = createDataMap(formData);

    // Merge descItemTypeGroup
    // Procházíme předpisy group a type! - nikoli hodnoty z db, ty pouze připojujeme
    var descItemGroups = [];
    data.descItemTypeGroups.forEach(group => {
        var resultGroup = {
            hasFocus: false,
            ...dataMap.groupMap.find(group.code),
            ...group,
            descItemTypes: []
        };

        // Merge descItemType
        group.descItemTypes.forEach(descItemType => {
            var resultDescItemType = {
                hasFocus: false,
                ...dataMap.typeMap.find(descItemType.id),
                ...descItemType,
                descItems: []
            }

            // Merge descItem 
            // - DB verze
            // - původní verze descItem - data, která jsou aktuálně ve store
            var newDescItemType = descItemTypesMap[descItemType.id];
            var prevDescItemType = dataMap.typeMap.get(descItemType.id);
            
            if (mergeDescItems(resultDescItemType, prevDescItemType, newDescItemType, descItemTypeInfos)) {
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

    return formData;
}

export function updateFormData(state, data, rulDataTypes) {
    var currentNodeVersionId = state.data ? state.data.node.version : -1;
    var newNodeVersionId = data.node.version;
    state.data = data;

    // Mapa id descItemType na descItemType
    var descItemTypesMap = getDescItemTypesMap(data);

    // Seznam všech atributů - obecně, doplněný o rulDataType
    var typesInfo = getDescItemTypeGroupsInfo(data, rulDataTypes);
    var descItemTypeInfos = typesInfo.descItemTypeInfos;
    state.descItemTypeGroupsMap = typesInfo.descItemTypeGroupsMap;
    state.descItemTypeInfos = descItemTypeInfos;

    // ##
    // # Vytvoření formuláře se všemi povinnými a doporučenými položkami, které jsou doplněné reálnými daty ze serveru
    // # Případně promístuní merge.
    // ##

    if (state.data) {
        //console.log("--- MERGE FORM DATA from", currentNodeVersionId, "to", newNodeVersionId);
    } else {
        //console.log("--- INIT FORM DATA to", newNodeVersionId);
    }
    if (currentNodeVersionId <= newNodeVersionId) { // rovno musí být, protože i když mám danou verzi, nemusím mít nově přidané povinné položky na základě aktuálně upravené mnou
        //console.log("--- FORM DATA RUN");
        var newFormData = merge(state.formData, data, descItemTypesMap, rulDataTypes, descItemTypeInfos);
        state.formData = newFormData;
    } else {
        //console.log("--- FORM DATA SKIPPED");
    }
}

export function createDescItem(descItemTypeInfo, addedByUser) {
    var result = {
        '@type': getDescItemType(descItemTypeInfo),
        prevValue: null,
        hasFocus: false,
        touched: false,
        visited: false,
        value: '',
        error: {hasError:false},
        addedByUser
    };

    if (descItemTypeInfo.useSpecification) {
        result.descItemSpecId = '';
    }

    return result;
}

export function getDescItemType(descItemTypeInfo) {
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