/** Akce pro DAO. */

import * as SimpleListActions from 'shared/list/simple/SimpleListActions';
import * as DetailActions from 'shared/detail/DetailActions';
import {WebApi} from 'actions/index.jsx';
import {fundSubNodeDaosInvalidate} from 'actions/arr/subNodeDaos.jsx';

const AREA_PACKAGE_LIST_SUFFIX = '.daoPackageList';
const AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX = '.daoUnassignedPackageList';
const AREA_DAO_PACKAGE_DETAIL_SUFFIX = '.daoPackageDetail';
const AREA_DAO_NODE_LIST_SUFFIX = '.nodeDaoList';
const AREA_DAO_NODE_LIST_ASSIGN_SUFFIX = '.nodeDaoListAssign';
const AREA_DAO_PACKAGE_LIST_SUFFIX = '.packageDaoList';

/**
 * Funkce na refresh všech seznamů DAO.
 */
export function changeAllDaos(nodeIds) {
    return (dispatch, getState) => {
        var state = getState();
        const funds = state.arrRegion.funds;
        funds.forEach(fund => {
            const versionId = fund.versionId;
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_PACKAGE_LIST_SUFFIX));
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX));
            dispatch(DetailActions.invalidate('fund[' + versionId + ']' + AREA_DAO_PACKAGE_DETAIL_SUFFIX));
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_DAO_NODE_LIST_SUFFIX));
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_DAO_NODE_LIST_ASSIGN_SUFFIX));
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_DAO_PACKAGE_LIST_SUFFIX));
            dispatch(SimpleListActions.invalidate('fund[' + versionId + ']' + AREA_DAO_PACKAGE_LIST_SUFFIX));
            dispatch(fundSubNodeDaosInvalidate(versionId, nodeIds));
        });
    };
}

/**
 * Načtení DAO pro node.
 * @param versionId verze AS
 * @param nodeId id node
 */
export function fetchNodeDaoListIfNeeded(versionId, nodeId) {
    return SimpleListActions.fetchIfNeeded(
        'fund[' + versionId + ']' + AREA_DAO_NODE_LIST_SUFFIX,
        {versionId, nodeId},
        (parent, filter) => {
            return WebApi.getFundNodeDaos(versionId, nodeId, true).then(json => ({rows: json, count: 0}));
        },
    );
}

/**
 * Načtení DAO pro node - alternativní.
 * @param versionId verze AS
 * @param nodeId id node
 */
export function fetchNodeDaoListAssignIfNeeded(versionId, nodeId) {
    return SimpleListActions.fetchIfNeeded(
        'fund[' + versionId + ']' + AREA_DAO_NODE_LIST_ASSIGN_SUFFIX,
        {versionId, nodeId},
        (parent, filter) => {
            return WebApi.getFundNodeDaos(versionId, nodeId, true).then(json => ({rows: json, count: 0}));
        },
    );
}

/**
 * Načtení DAO pro package.
 * @param versionId verze AS
 * @param nodeId id node
 * @param unassigned mají se vracet pouze nepřiřazené (true) nebo všechny (false)
 */
export function fetchDaoPackageDaoListIfNeeded(versionId, daoPackageId, unassigned) {
    return SimpleListActions.fetchIfNeeded(
        'fund[' + versionId + ']' + AREA_DAO_PACKAGE_LIST_SUFFIX,
        {versionId, daoPackageId, unassigned},
        (parent, filter) => {
            return WebApi.getPackageDaos(versionId, daoPackageId, unassigned, true).then(json => ({
                rows: json,
                count: 0,
            }));
        },
    );
}

/**
 * Načtení všech balíčků digitalizátů pro daný AS.
 * @param versionId verze AS
 */
export function fetchDaoPackageListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded(
        'fund[' + versionId + ']' + AREA_PACKAGE_LIST_SUFFIX,
        versionId,
        (parent, filter) => {
            return WebApi.findDaoPackages(versionId, filter.fulltext, false).then(json => ({rows: json, count: 0}));
        },
    );
}

/**
 * Filtr seznamu.
 * @param versionId verze AS
 * @param filter filtr
 */
export function filterDaoPackageList(versionId, filter) {
    return SimpleListActions.filter('fund[' + versionId + ']' + AREA_PACKAGE_LIST_SUFFIX, filter);
}

/**
 * Načtení nepřiřazených balíčků digitalizátů pro daný AS.
 * @param versionId verze AS
 */
export function fetchDaoUnassignedPackageListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded(
        'fund[' + versionId + ']' + AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX,
        versionId,
        (parent, filter) => {
            return WebApi.findDaoPackages(versionId, filter.fulltext, true).then(json => ({rows: json, count: 0}));
        },
    );
}

/**
 * Filtr seznamu.
 * @param versionId verze AS
 * @param filter filtr
 */
export function filterDaoUnassignedPackageList(versionId, filter) {
    return SimpleListActions.filter('fund[' + versionId + ']' + AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX, filter);
}

/**
 * Načtení detailu balíčku digitalizátů.
 * @param versionId verze AS
 * @param id id balíčku
 */
export function fetchDaoPackageDetailIfNeeded(versionId, id) {
    return DetailActions.fetchIfNeeded('fund[' + versionId + ']' + AREA_DAO_PACKAGE_DETAIL_SUFFIX, id, id => {
        return WebApi.getArrRequest(versionId, id);
    });
}
