import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import * as DetailActions from '../../shared/detail/DetailActions';
import { AdminFund, AdminFundsFilter } from 'typings/store';

export const AREA_ADMIN_FUNDS = 'adminRegion.funds';
export const AREA_ADMIN_FUND = 'adminRegion.fund';
export const ADMIN_FUNDS_PAGE_SIZE = 200;

export function fundsFilter(text: string, from: number, pageSize: number = ADMIN_FUNDS_PAGE_SIZE) {
    return SimpleListActions.filter(AREA_ADMIN_FUNDS, {from, pageSize, text})
}

export function fundsFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_ADMIN_FUNDS, null, (parent?: unknown, filter: AdminFundsFilter = {}) =>
        {
            const {text, from, pageSize} = filter;
            return WebApi.findControlFunds(
                text || "", 
                pageSize, 
                from && from > 0 ? from : 0
            )
        }
    );
}

export function fundFetchIfNeeded(id: number) {
    return DetailActions.fetchIfNeeded(AREA_ADMIN_FUND, id, (id:number) => WebApi.getFundDetail(id));
}

export function selectFund(id: number) {
    return DetailActions.select(AREA_ADMIN_FUND, id);
}

export function setFund(fund: AdminFund) {
    return DetailActions.updateValue(AREA_ADMIN_FUND, fund.id, fund);
}
