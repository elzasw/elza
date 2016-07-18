import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

export function isFundFilesAction(action) {
    switch (action.type) {
        case types.FUND_FILES_REQUEST:
        case types.FUND_FILES_RECEIVE:
        case types.FUND_FILES_FILTER:
            return true;
        default:
            return false
    }
}

function _dataGridKey(state) {
    return '-filterText' + state.filterText
}

export function fetchFundFilesIfNeeded(versionId, fundId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId');
        if (!fund) {
            return
        }

        const fundFiles = fund.fundFiles;
        const dataKey = _dataGridKey(fundFiles);
        if (fundFiles.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey));

            WebApi.findFundFiles(fundId, fundFiles.filterText)
            .then(response => {
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId');
                if (newFund !== null) {
                    const newFundFiles = fund.fundFiles;
                    const newDataKey = _dataGridKey(newFundFiles);
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
        type: types.FUND_FILES_REQUEST,
        versionId,
        dataKey
    }
}

function _dataReceive(versionId, data) {
    return {
        type: types.FUND_FILES_RECEIVE,
        versionId,
        data
    }
}

export function fundFilesFilterByText(versionId, filterText) {
    return {
        type: types.FUND_FILES_FILTER,
        versionId,
        filterText
    }
}

export function fundFilesCreate(fundId, data, callback = null) {
    return (dispatch, getState) => {
        var formData = new FormData();

        for (var key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        formData.append("file", data.file[0]);
        formData.append("fundId", fundId);
        formData.append("@type", ".ArrFileVO");

        WebApi.createFundFile(formData)
            .then((json) => {
                callback && callback(json);
                dispatch(modalDialogHide())
            })
    }
}

export function fundFilesReplace(fileId, file) {
    return (dispatch, getState) => {

        var formData = new FormData();

        formData.append("file", file);
        formData.append("id", fileId);
        formData.append("@type", ".ArrFileVO");

        WebApi.updateFundFile(fileId, formData)
    }
}


export function fundFilesDelete(versionId, fundId, fileId) {
    return (dispatch, getState) => {
        WebApi.deleteArrFile(fileId)
    }
}
