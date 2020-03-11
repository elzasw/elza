/**
 * Akce pro hromadné akce
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';
import {indexById} from 'stores/app/utils.jsx';
import {ActionState} from '../../constants.tsx';
import i18n from '../../components/i18n';


export function actionStateTranslation(state) {
    switch (state) {
        case ActionState.RUNNING:
            return i18n('arr.fundAction.state.running');
        case ActionState.WAITING:
            return i18n('arr.fundAction.state.waiting');
        case ActionState.FINISHED:
            return i18n('arr.fundAction.state.finished');
        case ActionState.ERROR:
            return i18n('arr.fundAction.state.error');
        case ActionState.PLANNED:
            return i18n('arr.fundAction.state.planned');
        case ActionState.INTERRUPTED:
            return i18n('arr.fundAction.state.interrupted');
        case ActionState.OUTDATED:
            return i18n('arr.fundAction.state.outdated');
        default:
            return null;
    }
}


export function isFundActionAction(action) {
    switch (action.type) {
        case types.FUND_ACTION_ACTION_SELECT:
        case types.FUND_ACTION_CONFIG_REQUEST:
        case types.FUND_ACTION_CONFIG_RECEIVE:
        case types.FUND_ACTION_ACTION_DETAIL_REQUEST:
        case types.FUND_ACTION_ACTION_DETAIL_RECEIVE:
        case types.FUND_ACTION_LIST_REQUEST:
        case types.FUND_ACTION_LIST_RECEIVE:
        case types.FUND_ACTION_FORM_SHOW:
        case types.FUND_ACTION_FORM_HIDE:
        case types.FUND_ACTION_FORM_RESET:
        case types.FUND_ACTION_FORM_CHANGE:
        case types.FUND_ACTION_FORM_SUBMIT:
            return true;
        default:
            return false;
    }
}

export function fundActionFetchListIfNeeded(versionId) {
    return (dispatch, getState) => {
        const {arrRegion: {funds}} = getState();
        const index = indexById(funds, versionId, 'versionId');
        if (index !== null && funds[index].fundAction) {
            const {fundAction: {list: {currentDataKey, isFetching, fetched}}} = funds[index];
            if (currentDataKey !== versionId || (!isFetching && !fetched)) {
                dispatch(fundActionListRequest(versionId, versionId));
                WebApi.getBulkActionsList(versionId).then(data => dispatch(fundActionListReceive(versionId, versionId, data)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}
export function fundActionFetchConfigIfNeeded(versionId) {
    return (dispatch, getState) => {
        const {arrRegion: {funds}} = getState();
        const index = indexById(funds, versionId, 'versionId');
        if (index !== null && funds[index].fundAction) {
            const {fundAction: {config: {currentDataKey, isFetching, fetched}}} = funds[index];
            if (currentDataKey !== versionId || (!isFetching && !fetched)) {
                dispatch(fundActionConfigRequest(versionId, versionId));
                WebApi.getBulkActions(versionId).then(data => dispatch(fundActionConfigReceive(versionId, versionId, data)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}

export function fundActionFetchDetailIfNeeded(versionId) {
    return (dispatch, getState) => {
        const {arrRegion: {funds}} = getState();
        const index = indexById(funds, versionId, 'versionId');
        if (index !== null) {
            const {fundAction : {detail: {currentDataKey, data, isFetching, fetched} }, versionId} = funds[index];
            const isNotSameDataKey = data && currentDataKey !== data.id;
            if (((isNotSameDataKey && !isFetching) || (!isFetching && !fetched)) && currentDataKey !== null) {
                dispatch(fundActionActionRequest(versionId, currentDataKey));
                WebApi.getBulkAction(currentDataKey).then(data => dispatch(fundActionActionReceive(versionId, currentDataKey, data)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}

export function fundActionActionSelect(versionId, dataKey) {
    return {
        type: types.FUND_ACTION_ACTION_SELECT,
        dataKey,
        versionId
    };
}
export function funcActionActionInterrupt(bulkActionRunId) {
    return dispatch => {
        WebApi.interruptBulkAction(bulkActionRunId)
    }
}

export function fundActionListRequest(versionId, dataKey) {
    return {
        type: types.FUND_ACTION_LIST_REQUEST,
        dataKey,
        versionId
    };
}

export function fundActionListReceive(versionId, dataKey, data) {
    return {
        type: types.FUND_ACTION_LIST_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionConfigRequest(versionId, dataKey) {
    return {
        type: types.FUND_ACTION_CONFIG_REQUEST,
        dataKey,
        versionId
    };
}

export function fundActionConfigReceive(versionId, dataKey, data) {
    return {
        type: types.FUND_ACTION_CONFIG_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionActionRequest(versionId, dataKey) {
    return {
        type: types.FUND_ACTION_ACTION_DETAIL_REQUEST,
        dataKey,
        versionId
    };
}

export function fundActionActionReceive(versionId, dataKey, data) {
    return {
        type: types.FUND_ACTION_ACTION_DETAIL_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionFormShow(versionId) {
    return {
        type: types.FUND_ACTION_FORM_SHOW,
        versionId
    };
}

export function fundActionFormHide(versionId) {
    return {
        type: types.FUND_ACTION_FORM_HIDE,
        versionId
    };
}

export function fundActionFormReset(versionId) {
    return {
        type: types.FUND_ACTION_FORM_RESET,
        versionId
    };
}

export function fundActionFormChange(versionId, data) {
    return {
        type: types.FUND_ACTION_FORM_CHANGE,
        data,
        versionId
    };
}

export function fundActionFormSubmit(versionId, code, config) {
    return (dispatch, getState) => {
        const {arrRegion: {funds}} = getState();
        const index = indexById(funds, versionId, 'versionId');
        if (index !== null) {
            const {fundAction : {form, isFormVisible}, versionId} = funds[index];
            if (isFormVisible) {
                const nodeIds = form.nodes.map(node => node.id);
                if (code === "PERZISTENTNI_RAZENI") {
                    WebApi.queuePersistentSortByIds(versionId, form.code, nodeIds, config);
                } else {
                    WebApi.queueBulkActionWithIds(versionId, form.code, nodeIds);
                }
                dispatch({
                    type: types.FUND_ACTION_FORM_SUBMIT,
                    versionId
                })
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}
