import * as types from 'actions/constants/ActionTypes';

import {Toastr, i18n} from 'components';

export function changeConformityInfo(findingAidVersionId, nodeId) {
    return {
        type: types.CHANGE_CONFORMITY_INFO,
        findingAidVersionId: findingAidVersionId,
        nodeId: nodeId
    }
}

export function changeIndexingFinished() {

    Toastr.Actions.success({
        title: i18n("admin.fulltext.message.success"),
    });

    return {
        type: types.CHANGE_INDEXING_FINISHED
    }
}

export function changePackage() {

    return {
        type: types.CHANGE_PACKAGE
    }
}

export function changePackets(findingAidId) {

    return {
        type: types.CHANGE_PACKETS,
        findingAidId: findingAidId
    }
}

export function changeDescItem(versionId, nodeId, descItemObjectId) {

    return {
        type: types.CHANGE_DESC_ITEM,
        versionId,
        nodeId,
        descItemObjectId,
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
