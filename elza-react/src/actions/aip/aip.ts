import {WebApi} from 'actions/index.jsx';
import * as SimpleListActions from '../../shared/list/simple/SimpleListActions';
import * as DetailActions from '../../shared/detail/DetailActions';
import { Aip, AipFilterEntry, AipsFilter } from 'typings/store';
import {Api} from "../../api";
import {AipDetailVO, SearchParams, Sorting} from "elza-api";

export const AREA_AIPS = 'aipList';
export const AREA_AIP = 'aip';
export const AREA_SELECTED_AIPS = "selectedAips";
export const AIP_LOGICAL_TREE = "aipLogicalTree";
export const AREA_SELECTED_AIP_DAOS = "selectedAipDaos";
export const AREA_DAO_LINKS = "daoLinkList"
export const DEFAULT_PAGE_SIZE = 25;

export const aipsFilter = (
    filters: AipFilterEntry[],
    from: number,
    pageSize: number = DEFAULT_PAGE_SIZE,
    sort?: Sorting[],
) => {
    return SimpleListActions.filter(AREA_AIPS, {from, pageSize, filters, sort});
}

const searchParams = (filter: AipsFilter = {}): SearchParams => {
    const {filters, from, pageSize, sort} = filter;
    return {
        filters: (filters || []).map(entry => entry.filter),
        offset: from && from > 0 ? from : 0,
        size: pageSize,
        sort: sort || [],
    };
}

export const aipsFetchIfNeeded = (forceFetch = false) => {
    return SimpleListActions.fetchIfNeeded(AREA_AIPS, null, (parent?: unknown, filter: AipsFilter = {}) =>
            Api.aips.aipFindByFilter(searchParams(filter)).then(response => response.data),
        forceFetch
    );
}

/**
 * Zobrazení stránky, na které leží daný balíček.
 *
 * Seznam je stránkovaný a balíček může být kdekoliv v něm, takže stránku hledá server - jen on
 * ví, kolik balíčků se ve zvoleném řazení a filtru řadí před ním. Vrácená stránka se uloží jako
 * obyčejná odpověď seznamu, takže další stránkování a řazení pokračuje beze změny.
 *
 * Filtry obrazovky se předávají zvlášť, ne ze store: v okamžiku skoku tam ještě nemusí být a
 * stránka spočtená nad jiným seznamem by vedla jinam.
 *
 * @return true, pokud balíček ve filtru je; jinak se vrátí první stránka a je na volajícím,
 *         aby uživateli řekl, proč balíček nevidí
 */
export const aipsFocus = (
    aipId: number,
    filters: AipFilterEntry[],
    pageSize: number = DEFAULT_PAGE_SIZE,
    sort?: Sorting[],
) => {
    return async (dispatch: (action: unknown) => unknown): Promise<boolean> => {
        const params = searchParams({filters, from: 0, pageSize, sort});
        const {data} = await Api.aips.aipFindByFilter(params, aipId);

        dispatch(aipsFilter(filters, data.offset ?? 0, pageSize, sort));
        dispatch(SimpleListActions.setData(AREA_AIPS, null, data.rows, data.count));
        return data.focusFound ?? false;
    };
}

export function aipFetchIfNeeded(id: number, forceFetch = false) {
    return DetailActions.fetchIfNeeded(AREA_AIP, id, (id: number) => WebApi.getAip(id), forceFetch);
}

export function selectAip(id: number | string) {
    return DetailActions.select(AREA_AIP, id);
}

export function setAip(aip: Aip) {
    return DetailActions.updateValue(AREA_AIP, aip.id, aip);
}

export const setSelectedAips = (aips: AipDetailVO[]) => {
    return SimpleListActions.setData(AREA_SELECTED_AIPS, null, aips);
}

export const setSelectedAipDaos = (daDaoIds: number[]) => {
    return SimpleListActions.setData(AREA_SELECTED_AIP_DAOS, daDaoIds, daDaoIds);
}

export const fetchAipLogicalTreeIfNeeded = (ids: number[]) => {
    return DetailActions.fetchIfNeeded(AIP_LOGICAL_TREE, ids, () => WebApi.getAipsLogicalTree(ids))
}

export const daoLinksFetchIfNeeded = (nodeId: number, forceFetch = false) => {
    return DetailActions.fetchIfNeeded(AREA_DAO_LINKS, nodeId, () => Api.aips.aipGetDaoLinks(nodeId), forceFetch);
}
