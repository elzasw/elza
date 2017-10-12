import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from "../../shared/list/simple/SimpleListActions";
export const AREA_ADMIN_FUNDS = 'adminRegion.funds';

export function fundsFilter(text) {
    return SimpleListActions.filter(AREA_ADMIN_FUNDS, { text });
}

export function fundsFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_ADMIN_FUNDS, null, (parent, filter) => WebApi.findControlFunds(filter.text));
}
