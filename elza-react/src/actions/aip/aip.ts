import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import * as DetailActions from '../../shared/detail/DetailActions';
import { Aip, AipsFilter } from 'typings/store';

export const AREA_AIPS = 'aipList';
export const AREA_AIP = 'aip';
export const ADMIN_AIP_SIZE = 200;

export function aipsFilter(text: string, from: number, pageSize: number = ADMIN_AIP_SIZE) {
    return SimpleListActions.filter(AREA_AIPS, {from, pageSize, text})
}

export function aipsFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_AIPS, null, (parent?: unknown, filter: AipsFilter = {}) =>
        {
            const {text, from, pageSize} = filter;
            return WebApi.findAips(
                text || "",
                pageSize,
                from && from > 0 ? from : 0
            )
        }
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
