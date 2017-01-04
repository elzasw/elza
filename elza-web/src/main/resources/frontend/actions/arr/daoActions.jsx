/** Akce pro DAO. */

import * as SimpleListActions from "shared/list/simple/SimpleListActions";
import * as DetailActions from "shared/detail/DetailActions";
import {WebApi} from 'actions/index.jsx';

const AREA_DAO_PACKAGE_LIST_SUFFIX = ".daoPackageList";
const AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX = ".daoUnassignedPackageList";
const AREA_DAO_PACKAGE_DETAIL_SUFFIX = ".daoPackageDetail";
const AREA_DAO_LIST_SUFFIX = ".daoList";

/**
 * Načtení DAO pro node.
 * @param versionId verze AS
 * @param nodeId id node
 */
export function fetchNodeDaoListIfNeeded(versionId, nodeId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DAO_LIST_SUFFIX, {versionId, nodeId}, (parent, filter) => {
        return WebApi.getFundNodeDaos(versionId, nodeId, true)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení DAO pro package.
 * @param versionId verze AS
 * @param nodeId id node
 */
export function fetchDaoPackageDaoListIfNeeded(versionId, daoPackageId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DAO_LIST_SUFFIX, {versionId, daoPackageId}, (parent, filter) => {
        return WebApi.getPackageDaos(versionId, daoPackageId, true)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení všech balíčků digitalizátů pro daný AS.
 * @param versionId verze AS
 */
export function fetchDaoPackageListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DAO_PACKAGE_LIST_SUFFIX, versionId, (parent, filter) => {
        return WebApi.findDaoPackages(versionId, filter.fulltext, false)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení nepřiřazených balíčků digitalizátů pro daný AS.
 * @param versionId verze AS
 */
export function fetchDaoUnassignedPackageListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DAO_UNASSIGNED_PACKAGE_LIST_SUFFIX, versionId, (parent, filter) => {
        return WebApi.findDaoPackages(versionId, filter.fulltext, true)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení detailu balíčku digitalizátů.
 * @param versionId verze AS
 * @param id id balíčku
 */
export function fetchDaoPackageDetailIfNeeded(versionId, id) {
    return DetailActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DAO_PACKAGE_DETAIL_SUFFIX, id, (id) => {
        return WebApi.getArrRequest(versionId, id);
    });
}
