import {indexById} from 'stores/app/utils.jsx'
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'

function getDbItemTypesMap(data) {
    // Mapa id descItemType na descItemType
    var typesMap = {};
    data.groups.forEach(group => {
        group.types.forEach(type => {
            typesMap[type.id] = type;
        })
    })
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

export function createImplicitDescItem(descItemType, refType, addedByUser=false) {
    var descItem = createDescItem(descItemType, refType, addedByUser);
    descItem.position = 1;
    return descItem;
}

export function getFocusDescItemLocation(subNodeFormStore) {
    const formData = subNodeFormStore.formData

    for (var g=0; g<formData.descItemGroups.length; g++) {
        const group = formData.descItemGroups[g]
        if (group.hasFocus) {
            for (var dit=0; dit<group.descItemTypes.length; dit++) {
                const descItemType = group.descItemTypes[dit]
                if (descItemType.hasFocus) {
                    for (var di=0; di<descItemType.descItems.length; di++) {
                        const descItem = descItemType.descItems[di]
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

export function createDescItemFromDb(descItemType, descItem) {
    var result = {
        ...descItem,
        prevDescItemSpecId: descItem.descItemSpecId,
        prevValue: descItem.value,
        hasFocus: false,
        touched: false,
        visited: false,
        error: {hasError:false}
    }

    initFormKey(descItemType, result)

    return result
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

var _formKeys = {}
function initFormKey(descItemType, descItem) {
    if (descItem.formKey) {
        return
    }

    if (!_formKeys[descItemType.id]) {
        _formKeys[descItemType.id] = 1
    }
    descItem.formKey = 'fk_' + _formKeys[descItemType.id]
    _formKeys[descItemType.id] = _formKeys[descItemType.id] + 1
}

// 1. Doplní povinné a doporučené specifikace s prázdnou hodnotou, pokud je potřeba
// 2. Pokud atribut nemá žádnou hodnotu, přidá první implicitní
// 
export function consolidateDescItems(resultDescItemType, infoType, refType, addedByUser, emptySystemSpecToKeyMap={}) {
    var forceVisibility = infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED'

    // Vynucené hodnoty se specifikací, pokud je potřeba
    addForcedSpecifications(resultDescItemType, infoType, refType, emptySystemSpecToKeyMap)

    // Přidáme jednu hodnotu - chceme i u opakovatelného, pokud žádnou nemá (nebyla hodnota přifána vynucením specifikací)
    if (resultDescItemType.descItems.length === 0) {
        resultDescItemType.descItems.push(createImplicitDescItem(resultDescItemType, refType, addedByUser));
    }
    
    resultDescItemType.descItems.forEach((descItem, index) => {descItem.position = index + 1});
}

/**
 * Doplnění prázdných hodnot se specifikací, které jsou vynucené podle typu (REQUIRED a RECOMMENDED), pokud ještě v resultDescItemType nejsou.
 * Uvažujeme POUZE descItemType, které mají specifikaci a MAJÍ i hodnotu, né pouze specifikaci.
 */
export function addForcedSpecifications(resultDescItemType, infoType, refType, emptySystemSpecToKeyMap={}) {
    if (!refType.useSpecification) {
        return
    }

    if (!hasDescItemTypeValue(refType.dataType)) {
        return
    }

    // Seznam existujících specifikací
    var existingSpecIds = {}
    resultDescItemType.descItems.forEach(descItem => {
        if (typeof descItem.descItemSpecId !== 'undefined' && descItem.descItemSpecId !== '') {
            existingSpecIds[descItem.descItemSpecId] = true
        }
    })

    infoType.specs.forEach(spec => {
        const infoSpec = infoType.descItemSpecsMap[spec.id]
        var forceVisibility = infoSpec.type == 'REQUIRED' || infoSpec.type == 'RECOMMENDED'
        if (forceVisibility && !existingSpecIds[spec.id]) {  // přidáme ji na formulář, pokud má být vidět a ještě na formuláři není
            var descItem = createImplicitDescItem(resultDescItemType, refType, false)
            descItem.descItemSpecId = spec.id

            // Ponechání původního form key, pokud existovala tato položka již na klientovi a nebylo na ní šáhnuto
            var formKey = emptySystemSpecToKeyMap[spec.id]
            if (formKey) {
                descItem.formKey = formKey
            }

            // U vícehodnotových přidáváme všechny, které neexistují, u jednohodnotového nesmí být více než jedna
            if (infoType.rep === 1) {   // Vícehodnotový
                resultDescItemType.descItems.push(descItem)
            } else {    // Jednohodnotový, přidáme jen jednu
                if (resultDescItemType.descItems.length === 0) {    // není žádná, přidáme první
                    resultDescItemType.descItems.push(descItem)
                }
            }
        }
    })
}

function mergeDescItems(state, resultDescItemType, prevType, newType) {
    var infoType = state.infoTypesMap[resultDescItemType.id]
    var refType = state.refTypesMap[resultDescItemType.id]
    var forceVisibility = infoType.type == 'REQUIRED' || infoType.type == 'RECOMMENDED'

    if (!prevType) {    // ještě ji na formuláři nemáme
        if (!newType) { // není ani v DB, přidáme ji pouze pokud je nastaveno forceVisibility
            if (forceVisibility) {  // přidáme ji pouze pokud je nastaveno forceVisibility
                // Upravení a opravení seznamu hodnot, případně přidání rázdných
                consolidateDescItems(resultDescItemType, infoType, refType, false)

                return true;
            }
        } else {    // je v db a není předchozí, dáme ji do formuláře bez merge
            newType.descItems.forEach(descItem => {
                resultDescItemType.descItems.push(createDescItemFromDb(resultDescItemType, descItem))
            })

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            consolidateDescItems(resultDescItemType, infoType, refType, false)

            return true;
        }
    } else {    // již ji na formuláři máme, musíme provést merge
        if (!newType) { // není ani v DB, my jí máme, musíme nechat jen nově přidané hodnoty, protože ostatní i mnou editované již někdo smazal (protože nepřišel objekt newType)
            prevType.descItems.forEach(descItem => {
                if (typeof descItem.id === 'undefined' && descItem.addedByUser) { // mnou přidaná ještě neuložená, necháme je
                    resultDescItemType.descItems.push(descItem);
                }
            })

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            if (forceVisibility) {
                consolidateDescItems(resultDescItemType, infoType, refType, false)
            }

            // Chceme ji pokud má nějaké hodnoty
            if (resultDescItemType.descItems.length > 0) {
                return true
            }
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

            // Nakopírování nově přijatých hodnot, případně ponechání stejných (na základě descItemObjectId a prev value == value ze serveru, které již uživatel upravil a nejsou odeslané)
            newType.descItems.forEach(descItem => {
                var prevDescItem = prevDescItemMap[descItem.descItemObjectId];

                if (prevDescItem && prevDescItemHasSamePrevValue(prevDescItem, descItem) && prevDescItem.touched) {   // původní hodnota přijatá ze serveru má stejné hodnoty jako jsou nyní v nově přijatých datech na serveru a uživatel nám aktuální data upravil
                    var item = prevDescItem;
                    addUid(item, null);
                    item.formKey = prevDescItem.formKey
                    resultDescItemType.descItems.push(item)
                } else {
                    var item = createDescItemFromDb(resultDescItemType, descItem);
                    addUid(item, null);
                    if (prevDescItem) {
                        item.formKey = prevDescItem.formKey
                    } else {
                        initFormKey(resultDescItemType, item)
                    }
                    resultDescItemType.descItems.push(item)
                }
            })

            // Doplnění o přidané a neuložené v aktuálním klientovi
            // Pokud se jedná o jednohodnotvý atribut, necháme jen tu ze serveru
            var emptySystemSpecToKeyMap = {}   // mapa id specifikace prázdné systémové vynucené položky specifikace na formKey dané položky - aby nám položky na formuláři neskákaly
            if (infoType.rep === 1) {   // Vícehodnotový
                var prevDescItem = null;
                prevType.descItems.forEach((descItem, index) => {
                    addUid(descItem, index);

                    if (typeof descItem.id === 'undefined') { // mnou přidaná ještě neuložená, musíme jí přidat na správné místo
                        // Pokud se jedná o systémově přidanou hodnotu a uživatel na ní zatím nešáhl, nebudeme ji vůbec uvažovat
                        if (!descItem.addedByUser && !descItem.touched) {    // systémově přidaná a neupravená
                            // nebudeme ji uvažovat, jen se pro ni budeme snažit zachovat formKey, aby nám položky na formuláři neskákaly - jedná se o systémnově přidané atributy s povinnou nebo doporučenou specifikací
                            if (refType.useSpecification && hasDescItemTypeValue(refType.dataType)) {
                                emptySystemSpecToKeyMap[descItem.descItemSpecId] = descItem.formKey
                            }
                        } else {
                            if (prevDescItem) { // má předchozí, zkusíme ji v novém rozložení dát na stejné místo, pokud to půjde
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
                    }

                    prevDescItem = descItem;
                })
            }

            // Upravení a opravení seznamu hodnot, případně přidání rázdných
            consolidateDescItems(resultDescItemType, infoType, refType, false, emptySystemSpecToKeyMap)

            return true;
        }
    }

    // Uměle doplníme ty specifikace, které 

    return false;
}

function merge(state) {
    // Načten data map pro aktuální data, která jsou ve store - co klient zobrazuje (nemusí být, pokud se poprvé zobrazuje formulář)
    var dataMap = createDataMap(state.formData);

    // Mapa db id descItemType na descItemType
    var dbItemTypesMap = {}
    state.data.groups.forEach(group => {
        group.types.forEach(type => {
            dbItemTypesMap[type.id] = type
        })
    })

    // Procházíme všechny skupiny, které mohou být na formuláři - nikoli hodnoty z db, ty pouze připojujeme
    // Všechny procházíme z toho důvodu, že některé mohou být vynuceny na zobrazení - forceVisible a klient je musí zobrazit
    var descItemGroups = [];
    state.infoGroups.forEach(group => {
        var resultGroup = {
            hasFocus: false,
            ...dataMap.groupMap.find(group.code),       // připojení skupiny již na klientovi, pokud existuje
            ...group,                                   // přepsání novými daty ze serveru
            descItemTypes: []
        };

        // Merge descItemType
        group.types.forEach(descItemType => {
            var resultDescItemType = {
                hasFocus: false,
                ...dataMap.typeMap.find(descItemType.id),   // připojení atributu již na klientovi, pokud existuje
                ...descItemType,                            // přepsání novými daty ze serveru
                descItems: []
            }

            // Merge descItems
            // - DB verze
            // - původní verze descItem - data, která jsou aktuálně ve store
            var prevDescItemType = dataMap.typeMap.get(descItemType.id);    // verze na klientovi, pokud existuje
            var newDescItemType = dbItemTypesMap[descItemType.id];          // verze z db, pokud existuje
            
            if (mergeDescItems(state, resultDescItemType, prevDescItemType, newDescItemType)) {
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

var typesNumToStrMap = {}
typesNumToStrMap[3] = 'REQUIRED'
typesNumToStrMap[2] = 'RECOMMENDED'
typesNumToStrMap[1] = 'POSSIBLE'
typesNumToStrMap[0] = 'IMPOSSIBLE'

// refTypesMap - mapa id info typu na typ, je doplněné o dataType objekt - obecný číselník
export function updateFormData(state, data, refTypesMap) {
    // Přechozí a nová verze node
    var currentNodeVersionId = state.data ? state.data.node.version : -1;
    var newNodeVersionId = data.node.version;

    // ##
    // # Vytvoření formuláře se všemi povinnými a doporučenými položkami, které jsou doplněné reálnými daty ze serveru
    // # Případně promítnutí merge.
    // ##
    if (currentNodeVersionId <= newNodeVersionId) { // rovno musí být, protože i když mám danou verzi, nemusím mít nově přidané povinné položky na základě aktuálně upravené mnou
        // Data přijatá ze serveru
        state.data = data

        // Info skupiny - ty, které jsou jako celek definované pro konkrétní JP - obsahují všechny atributy včetně např. typu - POSSIBLE atp.
        // Změna číselného typu na řetězec
        // Přidání do info skupin position
        state.infoGroupsMap = {}
        state.infoTypesMap = {}                     // mapa id descItemTypeInfo na descItemTypeInfo
        state.infoGroups = data.typeGroups.map((group, index) => {
            var resultGroup = {
                ...group,
                position: index,
                types: group.types.map(type => {
                    var specs = type.specs.map(spec => {
                        var resultSpec = {
                            ...spec,
                            type: typesNumToStrMap[spec.type]
                        }
                        return resultSpec
                    })

                    var resultType = {
                        ...type,
                        type: typesNumToStrMap[type.type],
                        specs: specs,
                        descItemSpecsMap: getMapFromList(specs),
                    }

                    state.infoTypesMap[resultType.id] = resultType

                    return resultType
                })
            }

            state.infoGroupsMap[resultGroup.code] = resultGroup

            return resultGroup
        })

        // Mapa číselníku decsItemType
        state.refTypesMap = refTypesMap

        // Mapa id descItemType na descItemType - existujících dat ze serveru
        var dbItemTypesMap = getDbItemTypesMap(data)

        var newFormData = merge(state);
        state.formData = newFormData;
    }
}

export function createDescItem(descItemType, refType, addedByUser) {
    var result = {
        '@type': getItemType(refType.dataType),
        prevValue: null,
        hasFocus: false,
        touched: false,
        visited: false,
        value: null,
        error: {hasError:false},
        addedByUser
    };

    initFormKey(descItemType, result)

    if (refType.useSpecification) {
        result.descItemSpecId = '';
    }

    // Inicializační hodnoty pro nově vytvořenou položku
    switch (refType.dataType.code) {
        case "UNITDATE":
            result.calendarTypeId = 1;
        break;
        case "JSON_TABLE":
            result.value = { rows: [{ values: {} }] };
        break;
    }

    return result;
}

export function getItemType(dataType) {
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
        case 'PARTY_REF':
            return '.ArrItemPartyRefVO';
        case 'RECORD_REF':
            return '.ArrItemRecordRefVO';
        case 'PACKET_REF':
            return '.ArrItemPacketVO';
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
        default:
            console.error("Unsupported data type", dataType);
            return null;
    }
}