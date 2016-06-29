import * as types from 'actions/constants/ActionTypes.js';

import {i18n} from 'components/index.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

export function isFundChangeAction(action) {
    switch (action.type) {
        case types.CHANGE_VISIBLE_POLICY:
            return true
        default:
            return false
    }
}

export function changeConformityInfo(fundVersionId, nodeIds) {
    return {
        type: types.CHANGE_CONFORMITY_INFO,
        fundVersionId: fundVersionId,
        nodeIds: nodeIds
    }
}

export function changeIndexingFinished() {

    addToastrSuccess(i18n("admin.fulltext.message.success"));

    return {
        type: types.CHANGE_INDEXING_FINISHED
    }
}

export function changePackage() {

    return {
        type: types.CHANGE_PACKAGE
    }
}

export function changeInstitution() {

    return {
        type: types.CHANGE_INSTITUTION
    }
}

export function changePackets(fundId) {

    return {
        type: types.CHANGE_PACKETS,
        fundId: fundId
    }
}

export function changeFiles(fundId, name) {
    return {
        type: types.CHANGE_FILES,
        fundId: fundId
    }
}

export function changeNodes(versionId, nodeIds) {

    return {
        type: types.CHANGE_NODES,
        versionId,
        nodeIds
    }
}

export function changeDeleteLevel(versionId, nodeId, parentNodeId) {

    return {
        type: types.CHANGE_DELETE_LEVEL,
        versionId,
        nodeId,
        parentNodeId
    }
}

export function changeAddLevel(versionId, nodeId, parentNodeId) {

    return {
        type: types.CHANGE_ADD_LEVEL,
        versionId,
        nodeId,
        parentNodeId
    }
}

export function changeFundAction(versionId, id) {
    return {
        type: types.CHANGE_FUND_ACTION,
        versionId,
        id
    }
}

export function changeParty(partyId){
    return {
        type: types.PARTY_UPDATED,
        partyId: partyId
    }
}

export function changePartyDelete(partyId){
    return {
        type: types.CHANGE_PARTY_DELETED,
        partyId: partyId
    }
}


export function changeApproveVersion(fundId, versionId) {

    return {
        type: types.CHANGE_APPROVE_VERSION,
        versionId,
        fundId
    }
}

export function changeMoveLevel(versionId) {

    return {
        type: types.CHANGE_MOVE_LEVEL,
        versionId
    }
}

export function changeRegistryRecord(changedIds){
    return {
        type: types.CHANGE_REGISTRY_UPDATE,
        changedIds
    }
}

export function changeFund(fundId) {
    return {
        type: types.CHANGE_FUND,
        fundId
    }
}
export function deleteFund(fundId) {
    return {
        type: types.DELETE_FUND,
        fundId
    }
}

export function fundOutputChanges(versionId, outputIds) {
    return {
        type: types.OUTPUT_CHANGES,
        versionId,
        outputIds,
    }
}

export function fundOutputChangesDetail(versionId, outputIds) {
    return {
        type: types.OUTPUT_CHANGES_DETAIL,
        versionId,
        outputIds,
    }
}

export function changeFundRecord(versionId, nodeId, version) {
    return {
        type: types.CHANGE_FUND_RECORD,
        versionId,
        nodeId,
        version
    }
}

export function changeVisiblePolicy(versionId, nodeId, invalidateNodes) {
    var nodeIdsMap = {};

    nodeId.forEach(item => nodeIdsMap[item] = true);

    return {
        type: types.CHANGE_VISIBLE_POLICY,
        versionId,
        nodeId,
        nodeIdsMap,
        invalidateNodes
    }
}

export function fundOutputGenerated(versionId, outputId) {
    return {
        type: types.GENERATED_OUTPUT,
        versionId,
        outputId
    }
}