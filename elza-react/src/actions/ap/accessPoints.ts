import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import {  ApAccessPointFilter } from 'typings/store';


export const AREA_ACCESS_POINTS = "accessPoints";

export function accessPointFilter(text: string, from: number, pageSize: number = 1000) {
    return SimpleListActions.filter(AREA_ACCESS_POINTS, {from, pageSize, text})
}

export function accessPointsFetchIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_ACCESS_POINTS, null, (parent?: unknown, filter: ApAccessPointFilter = {}) =>
        {
            const {text, pageSize, from} = filter;
            return WebApi.getAccessPoints(
                text || "",
                pageSize,
                from && from > 0 ? from : 0
            )
        }
    );
}
