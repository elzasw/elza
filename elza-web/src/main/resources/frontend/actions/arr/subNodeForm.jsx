/**
 * Akce pro formulář JP.
 */

// Konfigurace velikosti cache dat pro formulář
const CACHE_SIZE = 20
const CACHE_SIZE2 = CACHE_SIZE/2

import {WebApi} from 'actions'
import {getMapFromList, indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'
import {valuesEquals} from 'components/Utils.jsx'
import {setFocus} from 'actions/global/focus'

import * as types from 'actions/constants/ActionTypes';

/**
 * Akce přidání nové prázdné hodnoty descItem vícehodnotového atributu descItemType.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění nové hodnoty
 */
export function faSubNodeFormValueAdd(versionId, nodeId, nodeKey, valueLocation) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_ADD,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
    }
}

/**
 * Akce validace hodnoty na serveru - týká se jen hodnot datace.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey Klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty pro validaci
 */
export function faSubNodeFormValueValidate(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        WebApi.validateUnitdate(loc.descItem.value)
            .then(json => {
                dispatch(faSubNodeFormValueValidateResult(versionId, nodeId, nodeKey, valueLocation, json));
            })
    }
}

/**
 * Akce propagace výsledku validace hodnoty ze serveru do store.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey Klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {Object} result výsledek validace
 */
function faSubNodeFormValueValidateResult(versionId, nodeId, nodeKey, valueLocation, result) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_VALIDATE_RESULT,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        result
    }
}

/**
 * Akce změny hodnotya její promítnutí do store, případné uložení na server, pokud je toto vynuceno parametrem forceStore.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey Klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {Object} value nová hodnota
 * @param {boolean} forceStore pokud je true, je hodnota i odeslána na server pro uložení
 */
export function faSubNodeFormValueChange(versionId, nodeId, nodeKey, valueLocation, value, forceStore) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_CHANGE,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
            value,
            dispatch
        })

        if (forceStore) {
            formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation)
        }
    }
}

/**
 * Akce změna pozice hodnoty vícehodnotového atributu - změna pořadí hodnot.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey Klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {boolean} index nový index hodnoty v rámci atributu
 */
export function faSubNodeFormValueChangePosition(versionId, nodeId, nodeKey, valueLocation, index) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        if (!loc.descItem.error.hasError && typeof loc.descItem.id !== 'undefined') {
            dispatch({
                type: types.FA_SUB_NODE_FORM_VALUE_CHANGE_POSITION,
                versionId,
                nodeId,
                nodeKey,
                valueLocation,
                index,
            })

            var descItem = {...loc.descItem, position: index + 1}

            WebApi.updateDescItem(versionId, subNodeForm.data.node.version, descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'UPDATE'));
                })
        }
    }
}

/**
 * Akce kopírování hodnot konkrétního atributu z předcházející JP.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeVersionId verze node
 * @param {int} descItemTypeId id atribtu
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění
 */
export function faSubNodeFormValuesCopyFromPrev(versionId, nodeId, nodeVersionId, descItemTypeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, true));
        WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId)
            .then(json => {
                dispatch(faSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, nodeId, nodeKey, valueLocation, json));
            })
    }
}

/**
 * Akce změna hodnoty atributu - odkaz na osoby.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {Object} value hodnota
 */
export function faSubNodeFormValueChangeParty(versionId, nodeId, nodeKey, valueLocation, value) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_CHANGE_PARTY,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
            value,
            dispatch
        })
    }
}

/**
 * Akce změna hodnoty atributu - odkaz na rejstřík.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {Object} value hodnota
 */
export function faSubNodeFormValueChangeRecord(versionId, nodeId, nodeKey, valueLocation, value) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_CHANGE_RECORD,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
            value,
            dispatch
        })
    }
}

/**
 * Akce změna hodnoty specifikace atributu.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 * @param {Object} value hodnota
 */
export function faSubNodeFormValueChangeSpec(versionId, nodeId, nodeKey, valueLocation, value) {
    return (dispatch, getState) => {
        // Dispatch zmněny specifikace
        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
            value,
        })

        // Vynucení uložení na server, pokud je validní jako celek
        formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation)
    }
}

/**
 * Test, zda je nutné hodnotu atributu uložit, např. pokud byla změněna nebo pokud ještě nebyla založena na serveru.
 * @param {Object} descItem hodnota atributu
 * @param {Object} refType ref typ atributu
 */
export function descItemNeedStore(descItem, refType) {
    if (!descItem.error.hasError && descItem.touched) {
        if (typeof descItem.id !== 'undefined') {
            // Jen pokud se hodnota nebo specifikace změnila
            var needUpdate = false;
            if (refType.useSpecification && !valuesEquals(descItem.descItemSpecId, descItem.prevDescItemSpecId)) {
                needUpdate = true;
            }
            if (!valuesEquals(descItem.value, descItem.prevValue)) {
                needUpdate = true;
            }
            if (!valuesEquals(descItem.calendarTypeId, descItem.prevCalendarTypeId)) {
                needUpdate = true;
            }

            return needUpdate
        } else {
            return true
        }
    }
    return false
}

/**
 * Odeslání hodnoty atributu na server - buď vytvoření nebo aktualizace.
 * @param {Object} dispatch odkaz na funkci dispatch
 * @param {Object} getState odkaz na funkci pro načtení store
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 */
function formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation) {
    var state = getState();
    var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
    var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

    var refType = subNodeForm.refTypesMap[loc.descItemType.id]

    if (descItemNeedStore(loc.descItem, refType)) {
        if (typeof loc.descItem.id !== 'undefined') {
            WebApi.updateDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'UPDATE'));
                })
        } else {
            WebApi.createDescItem(versionId, nodeId, subNodeForm.data.node.version, loc.descItemType.id, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'CREATE'));
                })
        }
    }
}

/**
 * Blur na hodnotě atributu.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 */
export function faSubNodeFormValueBlur(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_BLUR,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
            receivedAt: Date.now()
        });

        formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation)
    }
}

/**
 * Smazání hodnoty atributu.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 */
export function faSubNodeFormValueDelete(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_DELETE,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
        })

        if (typeof loc.descItem.id !== 'undefined') {
            WebApi.deleteDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'DELETE'));
                })
        }
    }
}

/**
 * Přidání atributu na formulář.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {int} descItemTypeId id atributu
 */
export function faSubNodeFormDescItemTypeAdd(versionId, nodeId, nodeKey, descItemTypeId) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD,
            versionId,
            nodeId,
            nodeKey,
            descItemTypeId
        })

        var state = getState()
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
        dispatch(setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemTypeId, descItemObjectId: null}))
    }
}

/**
 * Smazání atributu POUZE z formuláře, nikoli na serveru!
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění atributu
 * @param {bool} onlyDescItems pokud je true, pouze se odeberou hodnoty atributu, ale daný atribut na formuláři zůstane, pokud je false, odebere se i atribut
 */
function faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, onlyDescItems) {
    return {
        type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        onlyDescItems
    }
}

/**
 * Smazání atributu.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění atributu
 */
export function faSubNodeFormDescItemTypeDelete(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        var hasDescItemsForDelete = false;
        loc.descItemType.descItems.forEach(descItem => {
            if (typeof descItem.id !== 'undefined') {
                hasDescItemsForDelete = true;
            }
        });

        dispatch(faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, false));

        if (hasDescItemsForDelete) {
            WebApi.deleteDescItemType(versionId, subNodeForm.data.node.id, subNodeForm.data.node.version, loc.descItemType.id)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'DELETE_DESC_ITEM_TYPE'));
                })
        }
    }
}

/**
 * Informační akce o provedené operaci na serveru.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění atributu nebo hodnoty
 * @param {string} operationType typ operace, jedna z hodnot: hodnota atributu['UPDATE', 'CREATE', 'DELETE'], atribut['DELETE_DESC_ITEM_TYPE']
 */
function faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, descItemResult, operationType) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        operationType,
        descItemResult: descItemResult
    }
}

/**
 * Informační akce o provedené operaci kopírování hodnot atributu z předchozí JP.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění atributu
 * @param {Object} copySiblingResult nová nakopírovaná data - objekt ze serveru
 */
export function faSubNodeFormDescItemTypeCopyFromPrevResponse(versionId, nodeId, nodeKey, valueLocation, copySiblingResult) {
    return {
        type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        copySiblingResult
    }
}

/**
 * Focus na hodnotě atributu.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} valueLocation konkrétní umístění hodnoty
 */
export function faSubNodeFormValueFocus(versionId, nodeId, nodeKey, valueLocation) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_FOCUS,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
    }
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function faSubNodeFormFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);

        if (subNodeForm != null) {
            if ((!subNodeForm.fetched || subNodeForm.dirty) && !subNodeForm.isFetching) {
                return dispatch(faSubNodeFormFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

/**
 * Načtení server dat pro formulář node pro aktuálně předané parametry  s využitím cache - pokud jsou data v cache, použije je, jinak si vyžádá nová data a zajistí i nakešování okolí.
 * Odpovídá volání WebApi.getFaNodeForm, jen dále zajišťuje cache.
 * @param {Object} getState odkaz na funkci pro načtení store
 * @param {Object} dispatch odkaz na funkci dispatch
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @return {Object} promise pro vrácení nových dat
 */
function getNodeForm(getState, dispatch, versionId, nodeId, nodeKey) {
    var state = getState()
    var node = getNodeStore(state, versionId, nodeKey);
    if (node === null) return   // nemělo by nastat

    const subNodeFormCache = node.subNodeFormCache

    var data = subNodeFormCache.dataCache[nodeId]
    if (!data) {    // není v cache, načteme ji včetně okolí
        // ##
        // # Data pro cache, jen pokud již cache nenačítá
        // ##
        if (!subNodeFormCache.isFetching) {
            if (node.isNodeInfoFetching || !node.nodeInfoFetched || node.nodeInfoDirty) {   // nemáme platné okolí (okolní NODE) pro daný NODE, raději je načteme ze serveru; nemáme vlastně okolní NODE pro získání seznamu ID pro načtení formulářů pro cache
                //console.log('### READ_CACHE', 'around')

                dispatch(faSubNodeFormCacheRequest(versionId, nodeId, nodeKey))
                WebApi.getFaNodeFormsWithAround(versionId, nodeId, CACHE_SIZE2)
                    .then(json => {
                        dispatch(faSubNodeFormCacheResponse(versionId, nodeId, nodeKey, json.forms))
                    })
            } else {    // pro získání id okolí můžeme použít store
                // Načtení okolí položky
                var index = indexById(node.childNodes, nodeId)
                var left = node.childNodes.slice(Math.max(index - CACHE_SIZE2, 0), index)
                var right = node.childNodes.slice(index, index + CACHE_SIZE2)

                var idsForFetch = []
                left.forEach(n => {
                    if (!subNodeFormCache.dataCache[n.id]) {
                        idsForFetch.push(n.id)
                    }
                })
                right.forEach(n => {
                    if (!subNodeFormCache.dataCache[n.id]) {
                        idsForFetch.push(n.id)
                    }
                })

                //console.log('### READ_CACHE', idsForFetch, node.childNodes, left, right)

                if (idsForFetch.length > 0) {   // máme něco pro načtení
                    dispatch(faSubNodeFormCacheRequest(versionId, nodeId, nodeKey))
                    WebApi.getFaNodeForms(versionId, idsForFetch)
                        .then(json => {
                            dispatch(faSubNodeFormCacheResponse(versionId, nodeId, nodeKey, json.forms))
                        })
                }
            }
        }

        // ##
        // # Data požadovaného formuláře
        // ##
        return WebApi.getFaNodeForm(versionId, nodeId)
    } else {    // je v cache, vrátíme ji
        //console.log('### USE_CACHE')
        return new Promise(function (resolve, reject) {
            resolve(data)
        })
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
function faSubNodeFormCacheRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_FORM_CACHE_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}

/**
 * Nová data byla načtena.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} formsMap objekt s daty
 */
function faSubNodeFormCacheResponse(versionId, nodeId, nodeKey, formsMap) {
    return {
        type: types.FA_SUB_NODE_FORM_CACHE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        formsMap
    }
}

/**
 * Nové načtení dat.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function faSubNodeFormFetch(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormRequest(versionId, nodeId, nodeKey))
        getNodeForm(getState, dispatch, versionId, nodeId, nodeKey)
            .then(json => {
                var state = getState()
                var subNodeForm = getSubNodeFormStore(state, versionId, nodeKey);
                if (subNodeForm.fetchingId == nodeId) {
                    dispatch(faSubNodeFormReceive(versionId, nodeId, nodeKey, json, state.refTables.rulDataTypes, state.refTables.descItemTypes))
                }
            })
    }
}

/**
 * Nová data byla načtena.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} json objekt s daty
 * @param {Object} rulDataTypes store - datové typy pro atributy
 * @param {Object} descItemTypes store - obecný předpis atributů - ref
 */
export function faSubNodeFormReceive(versionId, nodeId, nodeKey, json, rulDataTypes, descItemTypes) {
    return {
        type: types.FA_SUB_NODE_FORM_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        data: json,
        rulDataTypes,
        refDescItemTypes: descItemTypes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} versionId verze AP
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function faSubNodeFormRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_FORM_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}

/**
 * Načtení subNodeForm store.
 * @param {Object} state globální store
 * @param {int} versionId verze AP
 * @param {int} nodeKey klíč záložky
 * @return subNodeForm store
 */
function getSubNodeFormStore(state, versionId, nodeKey) {
    var node = getNodeStore(state, versionId, nodeKey);
    if (node !== null) {
        return node.subNodeForm;
    } else {
        return null;
    }
}

/**
 * Načtení node store pro předaná data.
 * @param {Object} state globální store
 * @param {int} versionId verze AP
 * @param {int} nodeKey klíč záložky
 * @return node store
 */
function getNodeStore(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node;
    }

    return null;
}