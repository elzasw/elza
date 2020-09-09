import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes';
import {objectById} from 'stores/app/utils.jsx';

export function isFundOutputFilesAction(action) {
    switch (action.type) {
        case types.FUND_OUTPUT_FILES_REQUEST:
        case types.FUND_OUTPUT_FILES_RECEIVE:
        case types.FUND_OUTPUT_FILES_FILTER:
            return true;
        default:
            return false;
    }
}

function _dataKey(outputResultId, state) {
    return outputResultId + '-filterText' + state.filterText;
}

export function fetchFundOutputFilesIfNeeded(versionId, outputId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId');
        if (!fund) {
            return;
        }

        const fundOutputFiles = fund.fundOutput.fundOutputFiles;
        if (!fundOutputFiles) {
            return;
        }
        const dataKey = _dataKey(outputId, fundOutputFiles);
        if (fundOutputFiles.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey));

            WebApi.findFundOutputFiles(outputId).then(response => {
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId');
                if (newFund !== null) {
                    const newFundOutputFiles = fund.fundOutput.fundOutputFiles;
                    const newDataKey = _dataKey(outputId, newFundOutputFiles);
                    if (newDataKey === dataKey) {
                        dispatch(_dataReceive(versionId, response));
                    }
                }
            });
        }
    };
}

function _dataRequest(versionId, dataKey) {
    return {
        type: types.FUND_OUTPUT_FILES_REQUEST,
        versionId,
        dataKey,
    };
}

function _dataReceive(versionId, data) {
    return {
        type: types.FUND_OUTPUT_FILES_RECEIVE,
        versionId,
        data,
    };
}

export function fundOutputFilesFilterByText(versionId, filterText) {
    return {
        type: types.FUND_OUTPUT_FILES_FILTER,
        versionId,
        filterText,
    };
}
