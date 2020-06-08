import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {objectById} from 'stores/app/utils.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {JAVA_ATTR_CLASS} from '../../constants';

export function isFundFilesAction(action) {
    switch (action.type) {
        case types.FUND_FILES_REQUEST:
        case types.FUND_FILES_RECEIVE:
        case types.FUND_FILES_FILTER:
            return true;
        default:
            return false;
    }
}

function _dataGridKey(state) {
    return '-filterText' + state.filterText;
}

export function fetchFundFilesIfNeeded(versionId, fundId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId');
        if (!fund) {
            return;
        }

        const fundFiles = fund.fundFiles;
        const dataKey = _dataGridKey(fundFiles);
        if (fundFiles.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey));

            WebApi.findFundFiles(fundId, fundFiles.filterText).then(response => {
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId');
                if (newFund !== null) {
                    const newFundFiles = fund.fundFiles;
                    const newDataKey = _dataGridKey(newFundFiles);
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
        type: types.FUND_FILES_REQUEST,
        versionId,
        dataKey,
    };
}

function _dataReceive(versionId, data) {
    return {
        type: types.FUND_FILES_RECEIVE,
        versionId,
        data,
    };
}

export function fundFilesFilterByText(versionId, filterText) {
    return {
        type: types.FUND_FILES_FILTER,
        versionId,
        filterText,
    };
}

export function fundFilesCreate(fundId, data, callback = null) {
    return (dispatch, getState) => {
        const formData = new FormData();

        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        if (data.file && data.file.length > 0) {
            formData.append('file', data.file[0]);
        }
        formData.append('fundId', fundId);
        formData.append(JAVA_ATTR_CLASS, '.ArrFileVO');

        return savingApiWrapper(
            dispatch,
            WebApi.createFundFileRaw(formData).then(json => {
                return callback && callback(json);
            }),
        );
    };
}

export function fundFilesReplace(fileId, file) {
    return (dispatch, getState) => {
        const formData = new FormData();

        if (file) {
            formData.append('file', file);
        }
        formData.append('id', fileId);
        formData.append(JAVA_ATTR_CLASS, '.ArrFileVO');

        savingApiWrapper(dispatch, WebApi.updateFundFileRaw(fileId, formData));
    };
}

export function fundFilesUpdate(fundId, fileId, data, callback = null) {
    return (dispatch, getState) => {
        const formData = new FormData();

        for (const key in data) {
            if (data.hasOwnProperty(key)) {
                formData.append(key, data[key]);
            }
        }
        if (data.file && data.file.length > 0) {
            formData.append('file', data.file[0]);
        }
        formData.append('id', fileId);
        formData.append('fundId', fundId);
        formData.append(JAVA_ATTR_CLASS, '.ArrFileVO');

        savingApiWrapper(
            dispatch,
            WebApi.updateFundFileRaw(fileId, formData).then(json => {
                return callback && callback(json);
            }),
        );
    };
}

export function fundFilesDelete(versionId, fundId, fileId) {
    return (dispatch, getState) => {
        WebApi.deleteArrFile(fileId);
    };
}
