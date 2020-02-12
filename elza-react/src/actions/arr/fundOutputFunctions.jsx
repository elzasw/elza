import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

export function isFundOutputFunctionsAction(action) {
    switch (action.type) {
        case types.FUND_OUTPUT_FUNCTIONS_REQUEST:
        case types.FUND_OUTPUT_FUNCTIONS_RECEIVE:
        case types.FUND_OUTPUT_FUNCTIONS_FILTER:
            return true;
        default:
            return false
    }
}

function _dataGridKey(state, outputId) {
    return outputId + '-filterRecommended' + state.filterRecommended
}

export function fetchFundOutputFunctionsIfNeeded(versionId, outputId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId');
        if (!fund || !outputId || !fund.fundOutput) {
            return
        }
        const fundOutputFunctions = fund.fundOutput.fundOutputFunctions;
        const dataKey = _dataGridKey(fundOutputFunctions, outputId);
        if (fundOutputFunctions.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey));

            WebApi.getFundOutputFunctions(outputId, fundOutputFunctions.filterRecommended)
            .then(response => {
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId');
                if (newFund !== null) {
                    const fundOutputDetail = fund.fundOutput.fundOutputDetail;
                    const newFundOutputFunctions = fund.fundOutput.fundOutputFunctions;
                    const newDataKey = _dataGridKey(newFundOutputFunctions, fundOutputDetail.id);
                    if (newDataKey === dataKey) {
                        dispatch(_dataReceive(versionId, response))
                    }
                }
            })
        }
    }
}

function _dataRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_FUNCTIONS_REQUEST,
        versionId,
        dataKey
    }
}

function _dataReceive(versionId, data) {
    return {
        type: types.FUND_OUTPUT_FUNCTIONS_RECEIVE,
        versionId,
        data
    }
}

export function fundOutputFunctionsFilterByState(versionId, filterRecommended) {
    return {
        type: types.FUND_OUTPUT_FUNCTIONS_FILTER,
        versionId,
        filterRecommended
    }
}

export function fundOutputActionRun(versionId, code) {
    return (dispatch, getState) => {
        const {arrRegion: {funds}} = getState();
        const index = indexById(funds, versionId, 'versionId');
        if (index !== null) {
            const {fundOutput : {fundOutputDetail}, versionId} = funds[index];
            if (fundOutputDetail) {
                const nodeIds = fundOutputDetail.nodes.map(node => node.id);
                return WebApi.queueBulkActionWithIds(versionId, code, nodeIds);
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}

export function fundOutputActionInterrupt(id) {
    return dispatch => {
        WebApi.interruptBulkAction(id)
    }
}
