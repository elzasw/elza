/**
 * Akce pro výstupy - named output.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions'
import {i18n} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

export function isFundOutput(action) {
    if (isFundOutputDetail(action)) {
        return true
    }
    
    switch (action.type) {
        case types.FUND_OUTPUT_REQUEST:
        case types.FUND_OUTPUT_RECEIVE:
            return true
        default:
            return false
    }
}

export function isFundOutputDetail(action) {
    switch (action.type) {
        case types.FUND_OUTPUT_DETAIL_REQUEST:
        case types.FUND_OUTPUT_DETAIL_RECEIVE:
        case types.FUND_OUTPUT_SELECT_OUTPUT:
        case types.FUND_OUTPUT_DETAIL_CLEAR:
            return true;
        default:
            return false
    }
}

function _fundOutputDataKey(fundOutput) {
    return true
}

function _fundOutputDetailDataKey(fundOutputDetail) {
    if (fundOutputDetail.id !== null) {
        return fundOutputDetail.id + '_'
    } else {
        return ''
    }
}

export function fundOutputUsageEnd(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputUsageEnd(versionId, outputId)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.output.title.usageEnded")));
            });        
    }
}

export function fundOutputAddNodes(versionId, outputId, nodeIds) {
    return (dispatch, getState) => {
        WebApi.fundOutputAddNodes(versionId, outputId, nodeIds)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.output.title.nodesAdded")));
                dispatch(modalDialogHide());
            });        
    }
}

export function fundOutputRemoveNodes(versionId, outputId, nodeIds) {
    return (dispatch, getState) => {
        WebApi.fundOutputRemoveNodes(versionId, outputId, nodeIds)
    }
}

export function fundOutputDelete(versionId, outputId) {
    return (dispatch, getState) => {
        WebApi.outputDelete(versionId, outputId)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.output.title.deleted")));
                dispatch(fundOutputDetailClear(versionId))
            });        
    }
}

export function fundOutputCreate(versionId, data) {
    return (dispatch, getState) => {
        WebApi.createOutput(versionId, data)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.output.title.added")));
                dispatch(modalDialogHide());
                dispatch(fundOutputSelectOutput(versionId, json.id))
            });        
    }
}

export function fundOutputEdit(versionId, outputId, data) {
    return (dispatch, getState) => {
        WebApi.updateOutput(versionId, outputId, data)
            .then((json) => {
                dispatch(modalDialogHide());
            });        
    }
}

export function fundOutputSelectOutput(versionId, id) {
    return {
        type: types.FUND_OUTPUT_SELECT_OUTPUT,
        versionId,
        id,
    }
}

function _getFundOutput(versionId, getState) {
    var state = getState();
    var index = indexById(state.arrRegion.funds, versionId, "versionId");
    if (index != null) {
        const fund = state.arrRegion.funds[index];
        return fund.fundOutput
    }

    return null
}

/**
 * Fetch dat pro detail výstupu.
 */
export function fundOutputDetailFetchIfNeeded(versionId, outputId) {
    return (dispatch, getState) => {
        const fundOutput = _getFundOutput(versionId, getState)
        if (fundOutput == null) {
            return
        }

        const fundOutputDetail = fundOutput.fundOutputDetail
        const dataKey = _fundOutputDetailDataKey(fundOutputDetail)

        if (fundOutputDetail.currentDataKey !== dataKey) {
            dispatch(fundOutputDetailRequest(versionId, dataKey))
            WebApi.getFundOutputDetail(versionId, outputId)
                .then(json => {
                    const newFundOutput = _getFundOutput(versionId, getState)
                    if (newFundOutput == null) {
                        return
                    }
                    const newFundOutputDetail = newFundOutput.fundOutputDetail
                    const newDataKey = _fundOutputDetailDataKey(newFundOutputDetail)
                    if (newDataKey === dataKey) {
                        dispatch(fundOutputDetailReceive(versionId, json))
                    }
                })
        }
    }
}

/**
 * Fetch dat pro seznam výstupů.
 */
export function fundOutputFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        const fundOutput = _getFundOutput(versionId, getState)
        if (fundOutput == null) {
            return
        }

        const dataKey = _fundOutputDataKey(fundOutput)

        if (fundOutput.currentDataKey !== dataKey) {
            dispatch(fundOutputRequest(versionId, dataKey))
            WebApi.getOutputs(versionId)
                .then(json => {
                    const newFundOutput = _getFundOutput(versionId, getState)
                    if (newFundOutput == null) {
                        return
                    }
                    const newDataKey = _fundOutputDataKey(newFundOutput)
                    if (newDataKey === dataKey) {
                        dispatch(fundOutputReceive(versionId, json))
                    }
                })
        }
    }
}

function fundOutputRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_REQUEST,
        versionId,
        dataKey,
    }
}

function fundOutputReceive(versionId, outputs) {
    return {
        type: types.FUND_OUTPUT_RECEIVE,
        versionId,
        outputs,
    }
}

function fundOutputDetailRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_DETAIL_REQUEST,
        versionId,
        dataKey,
    }
}

function fundOutputDetailReceive(versionId, data) {
    return {
        type: types.FUND_OUTPUT_DETAIL_RECEIVE,
        versionId,
        data,
    }
}


export function fundOutputDetailClear(versionId) {
    return {
        type: types.FUND_OUTPUT_DETAIL_CLEAR,
        versionId
    }
}