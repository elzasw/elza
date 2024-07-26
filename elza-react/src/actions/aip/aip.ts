import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import * as DetailActions from '../../shared/detail/DetailActions';
import { Aip, AipFilter, AipsFilter } from 'typings/store';
import { DaAipDetailVO } from 'api/DaAipDetailVO';

export const AREA_AIPS = 'aipList';
export const AREA_AIP = 'aip';
export const AREA_SELECTED_AIPS = "selectedAips";
export const DEFAULT_PAGE_SIZE = 25;

export const aipsFilter = (filters: AipFilter[], from: number, pageSize: number = DEFAULT_PAGE_SIZE) => {
    return SimpleListActions.filter(AREA_AIPS, {from, pageSize, filters});
}

export const aipsFetchIfNeeded = (forceFetch = false) => {
    return SimpleListActions.fetchIfNeeded(AREA_AIPS, null, (parent?: unknown, filter: AipsFilter = {}) =>
        {
            const {filters, from, pageSize} = filter;

            return WebApi.findAipsByFilter(
                filters || [],
                pageSize,
                from && from > 0 ? from : 0
            )
        },
        forceFetch
    );
}

export function aipFetchIfNeeded(id: number) {
    return DetailActions.fetchIfNeeded(AREA_AIP, id, (id:number) => WebApi.getAip(id));
}

export function selectAip(id: number | string) {
    return DetailActions.select(AREA_AIP, id);
}

export function setAip(aip: Aip) {
    return DetailActions.updateValue(AREA_AIP, aip.id, aip);
}

export const setSelectedAips = (aips: DaAipDetailVO[]) => {
    return SimpleListActions.setData(AREA_SELECTED_AIPS, null, aips);
}
