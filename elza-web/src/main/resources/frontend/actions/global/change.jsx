import * as types from 'actions/constants/ActionTypes';

import {i18n} from 'components';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions'

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

export function changeParty(partyId){
    return {
        type: types.PARTY_UPDATED,
        partyId: partyId
    }
}

export function changeApproveVersion(versionId) {

    return {
        type: types.CHANGE_APPROVE_VERSION,
        versionId
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

export function changeFundRecord(versionId, nodeId, version) {
    return {
        type: types.CHANGE_FUND_RECORD,
        versionId,
        nodeId,
        version
    }
}