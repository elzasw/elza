import * as types from '../../actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

export function isCustomFundAction(action) {
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

export function customFundActionSelectVersion(version, fundId) {
    return {
        type: types.CUSTOM_FUND_ACTION_SELECT_VERSION,
        version,
        fundId,
    };
}

export function customFundActionFetchListIfNeeded(fundId) {
    return (dispatch, getState) => {
        const { arrRegion: { customFund } } = getState();

        if (customFund.id !== fundId || customFund.versionId === null) {
            return WebApi.getFundDetail(fundId).then(fund => {
                for (const version of fund.versions) {
                    if (version.lockDate === null) {
                        dispatch(customFundActionSelectVersion(version, fund.id));
                        break;
                    }
                }
            });
        }
    };
}
