/**
 * Akce pro vyhledávání archivních souborů v komponentě Modal
 */
import { WebApi } from './../../actions/index.jsx';
import * as types from './../../actions/constants/ActionTypes';

export const getDataKey = (fulltext, isIdSearch) => {
    return isIdSearch ? `${fulltext}-id` : fulltext;
}

export function isFundSearchAction(action) {
    switch (action.type) {
        case types.FUND_SEARCH_FULLTEXT_CHANGE:
        case types.FUND_SEARCH_FULLTEXT_REQUEST:
        case types.FUND_SEARCH_FULLTEXT_RECEIVE:
        case types.FUND_SEARCH_EXPAND_FUND:
        case types.FUND_SEARCH_FUND_REQUEST:
        case types.FUND_SEARCH_FUND_RECEIVE:
            return true;
        default:
            return false;
    }
}

export function fundSearchFetchIfNeeded() {
    return (dispatch, getState) => {
        const {
            arrRegion: { fundSearch },
        } = getState();
        const { currentDataKey, isFetching, fulltext, funds, isIdSearch } = fundSearch;

        if (!fulltext) { return; }

        if (!isIdSearch) {
            if (fulltext != currentDataKey && !isFetching) {
                dispatch(fundSearchFulltextRequest());
                WebApi.fundFulltext(fulltext).then(result => {
                    dispatch(fundSearchFulltextReceive(result));
                });
            }

            funds.forEach(fund => {
                if (fund.expanded && !fund.isFetching && !fund.fetched) {
                    dispatch(fundSearchFundRequest(fund));
                    WebApi.fundFulltextNodes(fund.id).then(result => {
                        dispatch(fundSearchFundReceive(fund, result));
                    });
                }
            });
        }
        else {
            if (getDataKey(fulltext, isIdSearch) != currentDataKey && !isFetching) {
                dispatch(fundSearchFulltextRequest());
                WebApi.selectNode(fulltext).then(({ fund, nodeWithParent }) => {
                    const _fund = {
                        count: 1,
                        fundVersionId: fund.versions.find((version) => version.lockDate === null)?.id,
                        internalCode: fund.internalCode,
                        id: fund.id,
                        name: fund.name,
                        expanded: true,
                    };

                    if (_fund.fundVersionId != undefined) {
                        WebApi.getNodes(_fund.fundVersionId, [nodeWithParent.node.id]).then((nodes) => {
                            dispatch(fundSearchFulltextReceive([_fund]));
                            dispatch(fundSearchFundReceive(_fund, nodes));
                        })
                    }
                })
            }
        }
    };
}

export function fundSearchFulltextChange({ fulltext, isIdSearch }) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext,
        isIdSearch,
    };
}

export function fundSearchFulltextClear() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext: '',
    };
}

function fundSearchFulltextRequest() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_REQUEST,
    };
}

function fundSearchFulltextReceive(funds) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_RECEIVE,
        funds,
    };
}

function fundSearchFundRequest(fund) {
    return {
        type: types.FUND_SEARCH_FUND_REQUEST,
        fund,
    };
}

function fundSearchFundReceive(fund, nodes) {
    return {
        type: types.FUND_SEARCH_FUND_RECEIVE,
        fund,
        nodes,
    };
}

export function fundSearchExpandFund(fund) {
    return {
        type: types.FUND_SEARCH_EXPAND_FUND,
        fund,
    };
}
