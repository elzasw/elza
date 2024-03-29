import * as types from 'actions/constants/ActionTypes';

import React from 'react';
import { i18n } from 'components/shared';
import { Button } from '../../components/ui';
import { addToastr, addToastrDanger, addToastrInfo, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { fundOutputSelectOutput } from 'actions/arr/fundOutput';
import { routerNavigate } from 'actions/router';
import { indexById } from 'stores/app/utils';
import {
    AREA_EXT_SYSTEM_DETAIL,
    AREA_EXT_SYSTEM_LIST,
    extSystemDetailClear,
    extSystemDetailFetchIfNeeded,
    extSystemDetailInvalidate,
    extSystemListInvalidate,
} from 'actions/admin/extSystem';
import {
    detailInvalidate,
    detailUnselect,
    listInvalidate,
    preparedListInvalidate,
    queueListInvalidate,
} from 'actions/arr/arrRequestActions';
import { storeFromArea } from 'shared/utils';
import {
    AREA_REGISTRY_DETAIL,
    AREA_REGISTRY_LIST,
    registryDetailInvalidate,
    registryListInvalidate,
} from 'actions/registry/registry';
import { refExternalSystemListInvalidate } from 'actions/refTables/externalSystems';
import { structureTypeInvalidate } from '../arr/structureType';
import { DetailActions } from '../../shared/detail';
import { getFundVersion, urlFundOutputs } from "../../constants";
import { Link } from 'react-router-dom';

export function isFundChangeAction(action) {
    switch (action.type) {
        case types.CHANGE_VISIBLE_POLICY:
            return true;
        default:
            return false;
    }
}

export function changeConformityInfo(fundVersionId, nodeIds) {
    return {
        type: types.CHANGE_CONFORMITY_INFO,
        fundVersionId: fundVersionId,
        nodeIds: nodeIds,
    };
}

/**
 * Informace, že se změnil počet požadavků na digitalizaci u konkrétních node.
 * @param fundVersionId verze AS
 * @param nodeIds seznam id, u kterých došlo ke změně
 */
export function changeNodeRequests(fundVersionId, nodeIds) {
    return {
        type: types.CHANGE_NODE_REQUESTS,
        fundVersionId: fundVersionId,
        nodeIds: nodeIds,
    };
}

export function changeIndexingFinished() {
    addToastrSuccess(i18n('admin.fulltext.message.success'));

    return {
        type: types.CHANGE_INDEXING_FINISHED,
    };
}

export function changePackage() {
    return {
        type: types.CHANGE_PACKAGE,
    };
}

export function changeInstitution() {
    return {
        type: types.CHANGE_INSTITUTION,
    };
}

export function changePackets(fundId) {
    return {
        type: types.CHANGE_PACKETS,
        fundId: fundId,
    };
}

export function changeFiles(fundId, name) {
    return {
        type: types.CHANGE_FILES,
        fundId: fundId,
    };
}

export function changeNodes(versionId, nodeIds) {
    return {
        type: types.CHANGE_NODES,
        versionId,
        nodeIds,
    };
}

export function changeOutputs(versionId, outputIds) {
    return {
        type: types.CHANGE_OUTPUTS,
        versionId,
        outputIds,
    };
}

export function changeDeleteLevel(versionId, nodeId, parentNodeId) {
    return {
        type: types.CHANGE_DELETE_LEVEL,
        versionId,
        nodeId,
        parentNodeId,
    };
}

export function changeAddLevel(versionId, nodeId, parentNodeId) {
    return {
        type: types.CHANGE_ADD_LEVEL,
        versionId,
        nodeId,
        parentNodeId,
    };
}

export function changeFundAction(versionId, id) {
    return {
        type: types.CHANGE_FUND_ACTION,
        versionId,
        id,
    };
}

/**
 * Externí systémy CREATE
 *
 * @param extSystemId
 * @returns {function(*, *)}
 */
export function createExtSystem(extSystemId) {
    return (dispatch, getState) => {
        dispatch(extSystemListInvalidate());
        dispatch(extSystemDetailFetchIfNeeded(extSystemId));
        dispatch(refExternalSystemListInvalidate());
    };
}

/**
 * Externí systémy UPDATE
 *
 * @param extSystemId
 * @returns {function(*, *)}
 */
export function updateExtSystem(extSystemId) {
    return (dispatch, getState) => {
        const store = getState();
        const detail = storeFromArea(store, AREA_EXT_SYSTEM_DETAIL);
        const list = storeFromArea(store, AREA_EXT_SYSTEM_LIST);
        if (detail.id == extSystemId) {
            dispatch(extSystemDetailInvalidate());
        }

        if (list.rows && indexById(list.rows, extSystemId) !== null) {
            dispatch(extSystemListInvalidate());
        }
        dispatch(refExternalSystemListInvalidate());
    };
}

/**
 * Externí systémy DELETE
 *
 * @param extSystemId
 * @returns {function(*, *)}
 */
export function deleteExtSystem(extSystemId) {
    return (dispatch, getState) => {
        const store = getState();
        const detail = storeFromArea(store, AREA_EXT_SYSTEM_DETAIL);
        const list = storeFromArea(store, AREA_EXT_SYSTEM_LIST);
        if (detail.id == extSystemId) {
            dispatch(extSystemDetailClear());
        }
        if (list.rows && indexById(list.rows, extSystemId) !== null) {
            dispatch(extSystemListInvalidate());
        }
        dispatch(refExternalSystemListInvalidate());
    };
}

export function changeApproveVersion(fundId, versionId) {
    return {
        type: types.CHANGE_APPROVE_VERSION,
        versionId,
        fundId,
    };
}

export function changeMoveLevel(versionId) {
    return {
        type: types.CHANGE_MOVE_LEVEL,
        versionId,
    };
}

export function changeRegistry(changedIds) {
    return (dispatch, getState) => {
        const store = getState();
        const list = storeFromArea(store, AREA_REGISTRY_LIST);
        const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
        if (detail.id && changedIds.indexOf(detail.id) !== -1) {
            dispatch(registryDetailInvalidate());
        }
        if (list.data && list.data.filter(n => changedIds.indexOf(n) !== -1).length > 0) {
            dispatch(registryListInvalidate());
        }
    };
}

export function changeFund(fundId) {
    return {
        type: types.CHANGE_FUND,
        fundId,
    };
}

export function deleteFund(fundId) {
    return {
        type: types.DELETE_FUND,
        fundId,
    };
}

export function fundOutputChanges(versionId, outputIds) {
    return {
        type: types.OUTPUT_CHANGES,
        versionId,
        outputIds,
    };
}

export function fundInvalidChanges(fundIds, fundVersionIds) {
    return {
        type: types.FUND_INVALID,
        fundIds,
        fundVersionIds,
    };
}

export function fundOutputChangesDetail(versionId, outputIds) {
    return {
        type: types.OUTPUT_CHANGES_DETAIL,
        versionId,
        outputIds,
    };
}

export function changeFundRecord(versionId, nodeId, version) {
    return {
        type: types.CHANGE_FUND_RECORD,
        versionId,
        nodeId,
        version,
    };
}

export function changeVisiblePolicy(versionId, nodeId, invalidateNodes) {
    var nodeIdsMap = {};

    nodeId.forEach(item => (nodeIdsMap[item] = true));

    return {
        type: types.CHANGE_VISIBLE_POLICY,
        versionId,
        nodeId,
        nodeIdsMap,
        invalidateNodes,
    };
}

export function fundOutputStateChange(versionId, outputId, state) {
    return {
        type: types.OUTPUT_STATE_CHANGE,
        versionId,
        outputId,
        state,
    };
}

export function fundOutputStateChangeToastr(versionId, entityId, state) {
    return (dispatch, getState) => {
        const { arrRegion } = getState();
        if (arrRegion.activeIndex != null) {
            const fund = arrRegion.funds[arrRegion.activeIndex];
            if (fund === null || fund.versionId !== versionId) {
                return;
            }

            // show button only if on another page
            const isOutputVisible = window.location.pathname.endsWith(urlFundOutputs(fund.id, getFundVersion(fund), entityId));

            let showBtn;
            if (!isOutputVisible) {
                showBtn = (
                    <Link to={urlFundOutputs(fund.id, getFundVersion(fund), entityId)}>
                        {i18n('change.arr.output.clickToShow')}
                    </Link>
                );
            } else {
                showBtn = null;
            }

            switch (state) {
                case 'GENERATING':
                    return dispatch(addToastrInfo(i18n('change.arr.output.generating.title'), showBtn));
                case 'OUTDATED':
                    return dispatch(addToastrSuccess(
                        isOutputVisible
                            ? i18n('change.arr.output.outdated.title')
                            : <span dangerouslySetInnerHTML={{
                                __html: i18n('^change.arr.output.outdated.extended.title', fund.fundOutput.fundOutputDetail.name, fund.name)
                            }} />, showBtn));
                case 'FINISHED':
                    return dispatch(addToastrSuccess(
                        isOutputVisible
                            ? i18n('change.arr.output.finished.title')
                            : <span dangerouslySetInnerHTML={{
                                __html: i18n('^change.arr.output.finished.extended.title', fund.fundOutput.fundOutputDetail.name, fund.name)
                            }} />, showBtn, undefined, isOutputVisible ? undefined : null));
                case 'ERROR':
                    return dispatch(addToastrDanger(
                        isOutputVisible
                            ? i18n('change.arr.output.error.title')
                            : <span dangerouslySetInnerHTML={{
                                __html: i18n('^change.arr.output.error.extended.title', fund.fundOutput.fundOutputDetail.name, fund.name)
                            }} />, showBtn));
                default:
                    return;
            }
        }
    };
}

export function userChange(userIds) {
    return {
        type: types.CHANGE_USER,
        userIds,
    };
}

export function groupChange(ids) {
    return {
        type: types.CHANGE_GROUP,
        ids,
    };
}

export function groupDelete(id) {
    return {
        type: types.GROUP_DELETE,
        id,
    };
}

export function createRequest(value) {
    return (dispatch, getState) => {
        value.nodeIds && dispatch(changeNodes(value.versionId, value.nodeIds));
        dispatch(preparedListInvalidate(value.versionId));
        dispatch(listInvalidate(value.versionId));
    };
}

export function changeRequest(value) {
    return (dispatch, getState) => {
        value.nodeIds && dispatch(changeNodes(value.versionId, value.nodeIds));
        dispatch(preparedListInvalidate(value.versionId));
        dispatch(listInvalidate(value.versionId));
        dispatch(detailInvalidate(value.versionId, value.entityId));
    };
}

export function deleteRequest(value) {
    return (dispatch, getState) => {
        value.nodeIds && dispatch(changeNodes(value.versionId, value.nodeIds));
        dispatch(preparedListInvalidate(value.versionId));
        dispatch(listInvalidate(value.versionId));
        dispatch(detailUnselect(value.versionId, value.entityId));
    };
}

export function createRequestItemQueue(value) {
    return (dispatch, getState) => {
        dispatch(listInvalidate(value.versionId));
        dispatch(detailInvalidate(value.versionId, value.requestId));
        dispatch(queueListInvalidate());
    };
}

export function changeRequestItemQueue(value) {
    return (dispatch, getState) => {
        dispatch(listInvalidate(value.versionId));
        dispatch(queueListInvalidate());
        dispatch(detailInvalidate(value.versionId, value.requestId));
    };
}

export function nodesDelete(fundVersionId, nodeIds) {
    return {
        type: types.NODES_DELETE,
        versionId: fundVersionId,
        nodeIds: nodeIds,
    };
}

export function structureChange(data) {
    return (dispatch, getState) => {
        const store = getState();
        const list = storeFromArea(store, 'arrStructure');
        if (list && list.parent && list.parent.fundVersionId) {
            let funds = store.arrRegion.funds.filter(
                i => i.id == data.fundId && i.versionId == list.parent.fundVersionId,
            );
            if (funds.length > 0) {
                dispatch(structureTypeInvalidate());
                if (data.updateIds && data.updateIds.length > 0) {
                    for (let fund of funds) {
                        dispatch({
                            type: types.CHANGE_STRUCTURE,
                            fundId: data.fundId,
                            versionId: fund.versionId,
                            structureIds: data.updateIds,
                        });
                    }
                }
            }
        }
    };
}

export function changeAccessPoint(ids) {
    return (dispatch, getState) => {
        const store = getState();
        const parentAp = storeFromArea(store, AREA_REGISTRY_DETAIL);
        if (parentAp && ids.indexOf(parentAp.id) !== -1) {
            dispatch(DetailActions.invalidate(AREA_REGISTRY_DETAIL, null));
        }
    };
}
