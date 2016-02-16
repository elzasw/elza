import {WebApi} from 'actions'
import {getMapFromList, indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes';

export function faSubNodeFormValueAdd(versionId, nodeId, nodeKey, valueLocation) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_ADD,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
    }
}

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

export function faSubNodeFormValueValidateResult(versionId, nodeId, nodeKey, valueLocation, result) {
    return {
        type: types.FA_SUB_NODE_FORM_VALUE_VALIDATE_RESULT,
        versionId,
        nodeId,
        nodeKey,
        valueLocation,
        result
    }
}

// forceStore - true, pokud se mají data rovnou odeslat na server - není nutné focus do komponenty a následné blur 
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

export function faSubNodeFormValuesCopyFromPrev(versionId, nodeId, nodeVersionId, descItemTypeId, nodeKey, valueLocation) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormDescItemTypeDeleteInStore(versionId, nodeId, nodeKey, valueLocation, true));
        WebApi.copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId)
            .then(json => {
                dispatch(faSubNodeFormDescItemTypeDeleteResponse(versionId, nodeId, nodeKey, valueLocation, json));
            })
    }
}

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

function valuesEquals(v1, v2) {
    if (v1 === v2) {
        return true;
    }

    var v1empty = typeof v1 === 'undefined' || v1 === null || v1.length === 0
    var v2empty = typeof v2 === 'undefined' || v2 === null || v2.length === 0

    if (v1empty && v2empty) {
        return true
    }

    return false
}

function formValueStore(dispatch, getState, versionId, nodeId, nodeKey, valueLocation) {
    var state = getState();
    var subNodeForm = getSubNodeForm(state, versionId, nodeKey);
    var loc = subNodeForm.getLoc(subNodeForm, valueLocation);

    var refType = subNodeForm.refTypesMap[loc.descItemType.id]

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
    }
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
    return {
        type: types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD,
        versionId,
        nodeId,
        nodeKey,
        descItemTypeId
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

export function faSubNodeFormFetch(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        dispatch(faSubNodeFormRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaNodeForm(versionId, nodeId)
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
    // Doplnění descItemTypes o rulDataType
    var dataTypeMap = getMapFromList(rulDataTypes.items)
    descItemTypes.items.forEach(type => {
        type.dataType = dataTypeMap[type.dataTypeId]

        // Doplnění mapy id specifikace na specifikaci
        type.descItemSpecsMap = getMapFromList(type.descItemSpecs)
    })
    var refTypesMap = getMapFromList(descItemTypes.items)

    return {
        type: types.FA_SUB_NODE_FORM_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        data: json,
        refTypesMap,
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