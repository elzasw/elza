/**
 * Akce pro formulář JP.
 */

import {WebApi} from 'actions'
import {getMapFromList, indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'
import {valuesEquals} from 'components/Utils.jsx'
import {setFocus} from 'actions/global/focus'

import * as types from 'actions/constants/ActionTypes';

/**
 * Akce přidání nové prázdné hodnoty descItem vícehodnotového atributu descItemType.
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey Klíč záložky
 * {Object} valueLocation konkrétní umístění nové hodnoty
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
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey Klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty pro validaci
 */
export function faSubNodeFormValueValidate(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        WebApi.validateUnitdate(loc.descItem.value)
            .then(json => {
                dispatch(faSubNodeFormValueValidateResult(versionId, nodeId, nodeKey, valueLocation, json));
            })
    }
}

/**
 * Akce propagace výsledku validace hodnoty ze serveru do store.
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey Klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {Object} result výsledek validace
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
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey Klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {Object} value nová hodnota
 * {boolean} forceStore pokud je true, je hodnota i odeslána na server pro uložení
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
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey Klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {boolean} index nový index hodnoty v rámci atributu
 */
export function faSubNodeFormValueChangePosition(versionId, nodeId, nodeKey, valueLocation, index) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
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

            faSubNodeFormUpdateDescItem(versionId, subNodeForm.data.node.version, descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'UPDATE'));
                })
        }
    }
}

/**
 * Akce kopírování hodnot konkrétního atributu z předcházející JP.
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeVersionId verze node
 * {int} descItemTypeId id atribtu
 * {int} nodeKey klíč záložky
 * {Object} valueLocation konkrétní umístění
 */
export function faSubNodeFormValuesCopyFromPrev(versionId, nodeId, nodeVersionId, descItemTypeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, true));
        WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId)
            .then(json => {
                dispatch(faSubNodeFormDescItemTypeDeleteResponse(versionId, nodeId, nodeKey, valueLocation, json));
            })
    }
}

/**
 * Akce změna hodnoty atributu - odkaz na osoby.
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {Object} value hodnota
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
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {Object} value hodnota
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
 * {int} versionId verze AP
 * {int} nodeId id node záložky, které se to týká
 * {int} nodeKey klíč záložky
 * {Object} valueLocation konkrétní umístění hodnoty
 * {Object} value hodnota
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

function formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation) {
    var state = getState();
    var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
    var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

    var refType = subNodeForm.refTypesMap[loc.descItemType.id]

    if (descItemNeedStore(loc.descItem, refType)) {
        if (typeof loc.descItem.id !== 'undefined') {
            faSubNodeFormUpdateDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'UPDATE'));
                })
        } else {
            faSubNodeFormCreateDescItem(versionId, nodeId, subNodeForm.data.node.version, loc.descItemType.id, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'CREATE'));
                })
        }
    }
/*
    if (!loc.descItem.error.hasError && loc.descItem.touched) {
        if (typeof loc.descItem.id !== 'undefined') {
            // Jen pokud se hodnota nebo specifikace změnila
            var needUpdate = false;
            if (refType.useSpecification && !valuesEquals(loc.descItem.descItemSpecId, loc.descItem.prevDescItemSpecId)) {
                needUpdate = true;
            }
            if (!valuesEquals(loc.descItem.value, loc.descItem.prevValue)) {
                needUpdate = true;
            }
            if (!valuesEquals(loc.descItem.calendarTypeId, loc.descItem.prevCalendarTypeId)) {
                needUpdate = true;
            }
            if (needUpdate) {
                faSubNodeFormUpdateDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                    .then(json => {
                        dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'UPDATE'));
                    })
            }
        } else {
            faSubNodeFormCreateDescItem(versionId, nodeId, subNodeForm.data.node.version, loc.descItemType.id, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'CREATE'));
                })
        }
    }*/
}

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

export function faSubNodeFormValueDelete(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        dispatch({
            type: types.FA_SUB_NODE_FORM_VALUE_DELETE,
            versionId,
            nodeId,
            nodeKey,
            valueLocation,
        })

        if (typeof loc.descItem.id !== 'undefined') {
            faSubNodeFormDeleteDescItem(versionId, subNodeForm.data.node.version, loc.descItem)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'DELETE'));
                })
        }
    }
}

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
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
        dispatch(setFocus('arr', 2, 'subNodeForm', {descItemTypeId: descItemTypeId, descItemObjectId: null}))
    }
}

export function faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, onlyDescItems) {
    return {
        type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        onlyDescItems
    }
}

export function faSubNodeFormDescItemTypeDelete(versionId, nodeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
        var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

        var hasDescItemsForDelete = false;
        loc.descItemType.descItems.forEach(descItem => {
            if (typeof descItem.id !== 'undefined') {
                hasDescItemsForDelete = true;
            }
        });

        dispatch(faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, false));

        if (hasDescItemsForDelete) {
            faSubNodeFormDeleteDescItemType(versionId, subNodeForm.data.node.id, subNodeForm.data.node.version, loc.descItemType)
                .then(json => {
                    dispatch(faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, json, 'DELETE_DESC_ITEM_TYPE'));
                })
        }
    }
}

export function faSubNodeFormDescItemResponse(versionId, nodeId, nodeKey, valueLocation, descItemResult, operationType) {
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

export function faSubNodeFormDescItemTypeDeleteResponse(versionId, nodeId, nodeKey, valueLocation, copySiblingResult) {
    return {
        type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        copySiblingResult
    }
}

export function faSubNodeFormValueFocus(versionId, nodeId, nodeKey, valueLocation) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_FOCUS,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
    }
}

export function faSubNodeFormUpdateDescItem(versionId, nodeVersionId, descItem) {
    //console.log("ULOZENI desc item", versionId, nodeVersionId, descItem);
    return WebApi.updateDescItem(versionId, nodeVersionId, descItem);
}

export function faSubNodeFormCreateDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
    //console.log("VYTVORENI desc item", versionId, nodeId, nodeVersionId, descItemTypeId, descItem);
    return WebApi.createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem);
}

export function faSubNodeFormDeleteDescItem(versionId, nodeVersionId, descItem) {
    //console.log("SMAZANI desc item", versionId, nodeVersionId, descItem);
    return WebApi.deleteDescItem(versionId, nodeVersionId, descItem);
}

export function faSubNodeFormDeleteDescItemType(versionId, nodeId, nodeVersionId, descItemType) {
    //console.log("SMAZANI desc item type", versionId, nodeId, nodeVersionId, descItemType);
    return WebApi.deleteDescItemType(versionId, nodeId, nodeVersionId, descItemType.id);
}

export function faSubNodeFormFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);

        if (subNodeForm != null) {
            if ((!subNodeForm.fetched || subNodeForm.dirty) && !subNodeForm.isFetching) {
                return dispatch(faSubNodeFormFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

const CACHE_SIZE = 20
const CACHE_SIZE2 = CACHE_SIZE/2
function getNodeForm(getState, dispatch, versionId, nodeId, nodeKey) {
    var state = getState()
    var node = getNode(state, versionId, nodeKey);
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

function faSubNodeFormCacheRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_FORM_CACHE_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}
function faSubNodeFormCacheResponse(versionId, nodeId, nodeKey, formsMap) {
    return {
        type: types.FA_SUB_NODE_FORM_CACHE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        formsMap
    }
}

export function faSubNodeFormFetch(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormRequest(versionId, nodeId, nodeKey))
        //WebApi.getFaNodeForm(versionId, nodeId)
        getNodeForm(getState, dispatch, versionId, nodeId, nodeKey)
            .then(json => {
                var state = getState()
                var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
                if (subNodeForm.fetchingId == nodeId) {
                    dispatch(faSubNodeFormReceive(versionId, nodeId, nodeKey, json, state.refTables.rulDataTypes, state.refTables.descItemTypes))
                }
            })
    }
}

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

export function faSubNodeFormRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_FORM_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}

function getSubNodeForm(state, versionId, nodeKey) {
    var node = getNode(state, versionId, nodeKey);
    if (node !== null) {
        return node.subNodeForm;
    } else {
        return null;
    }
}
function getNode(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node;
    }

    return null;
}