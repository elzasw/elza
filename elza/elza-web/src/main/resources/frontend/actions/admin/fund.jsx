import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from "../../shared/list/simple/SimpleListActions";
import * as DetailActions from "../../shared/detail/DetailActions";

export const AREA_ADMIN_FUNDS = 'adminRegion.funds';
export const AREA_ADMIN_FUND = 'adminRegion.fund';

export function fundsFilter(text) {
    return SimpleListActions.filter(AREA_ADMIN_FUNDS, { text });
}

export function fundsFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_ADMIN_FUNDS, null, (parent, filter) => WebApi.findControlFunds(filter.text));
}

export function fundFetchIfNeeded(id) {
    return DetailActions.fetchIfNeeded(AREA_ADMIN_FUND, id, id => WebApi.getFundDetail(id));
}

export function selectFund(id) {
    return DetailActions.select(AREA_ADMIN_FUND, id);
}

export function setFund(fund) {
    return DetailActions.updateValue(AREA_ADMIN_FUND, fund.id, fund);
}
