// @ts-ignore
import AjaxUtils from '../components/AjaxUtils';
import { Api } from '../api';
import { CoordinateFileType, DEFAULT_LIST_SIZE, JAVA_ATTR_CLASS } from '../constants';
import {
    ArrRefTemplateEditVO,
    ArrRefTemplateMapTypeVO,
    ArrRefTemplateVO,
    CommentVO,
    CreateFund,
    FindFundsResult,
    IssueListVO,
    IssueStateVO,
    IssueVO,
    RowsResponse,
    UpdateFund,
    MapLayerVO,
} from '../types';
import { ApAccessPointCreateVO } from '../api/ApAccessPointCreateVO';
import { ApAccessPointVO } from '../api/ApAccessPointVO';
import { ApValidationErrorsVO } from '../api/ApValidationErrorsVO';
import { ApStateHistoryVO } from '../api/ApStateHistoryVO';
import { ApAttributesInfoVO } from '../api/ApAttributesInfoVO';
import { ApPartFormVO } from '../api/ApPartFormVO';
import { ApTypeVO } from '../api/ApTypeVO';
import { RulDataTypeVO } from '../api/RulDataTypeVO';
import { RulDescItemTypeExtVO } from '../api/RulDescItemTypeExtVO';
import { FilteredResultVO } from '../api/FilteredResultVO';
import { ApSearchType } from '../typings/globals';
import * as UrlBuilder from '../utils/UrlBuilder';
import { ArchiveEntityResultListVO } from '../api/ArchiveEntityResultListVO';
import type { ApAdvanceSearchFilter } from 'elza-api';
import { SyncsFilterVO } from '../api/SyncsFilterVO';
import { ExtSyncsQueueResultListVO } from '../api/ExtSyncsQueueResultListVO';
import { ApViewSettings } from '../api/ApViewSettings';
import { UsrUserVO } from '../api/UsrUserVO';
import {AipDetailVO} from "elza-api";

// @ts-ignore
const serverContextPath = window.serverContextPath;

function getData(data, timeout = 1000) {
    return new Promise(function(resolve, reject) {
        setTimeout(function() {
            resolve(data);
        }, timeout);
    });
}
/**
 * Zavolání webscoket operace na serveru.
 * @param url url
 * @param data data pro poslání
 * @param needResponse true, pokud se má čekat na návratové hodnoty ze serveru (včetně chybových stavů), v tuto chvíli chceme vždy
 * @return {Promise}
 */
function callWS(url, data, needResponse = true) {
    return new Promise((resolve, reject) => {
        if (needResponse) {
            // chceme skoro vždy
            // @ts-ignore
            window.ws.send(
                '/app' + url,
                JSON.stringify(data),
                successResponse => {
                    resolve(successResponse);
                },
                errorResponse => {
                    // příprava pro budoucí možnost odchytávání klientských výjimek - zavolá se error calbback
                    reject(errorResponse);
                },
            );
        } else {
            // @ts-ignore
            window.ws.send('/app' + url, JSON.stringify(data));
            resolve({});
        }
    });
}

/**
 * Web api pro komunikaci se serverem.
 */
export class WebApiCls {
    static baseUrl = '/api';
    static v1 = WebApiCls.baseUrl + '/v1';
    static aipV1 = WebApiCls.v1 + '/aip';
    static fundV1 = WebApiCls.v1 + '/fund';
    static authUrl = WebApiCls.baseUrl + '/auth';
    static arrangementUrl = WebApiCls.baseUrl + '/arrangement';
    static issueUrl = WebApiCls.baseUrl + '/issue';
    static registryUrl = WebApiCls.baseUrl + '/registry';
    static accesspointUrl = WebApiCls.v1 + '/accesspoint';
    static apUrl = WebApiCls.registryUrl;
    static daoUrl = WebApiCls.baseUrl + "/dao";
    static partyUrl = WebApiCls.baseUrl + '/party';
    static importUrl = WebApiCls.baseUrl + '/import';
    static exportUrl = WebApiCls.baseUrl + '/export';
    static actionUrl = WebApiCls.baseUrl + '/action';
    static kmlUrl = WebApiCls.baseUrl + '/kml';
    static ruleUrl = WebApiCls.baseUrl + '/rule';
    static changesUrl = WebApiCls.arrangementUrl + '/changes';
    static dmsUrl = WebApiCls.baseUrl + '/dms';
    static attachmentUrl = WebApiCls.baseUrl + '/attachment';
    static userUrl = WebApiCls.baseUrl + '/user';
    static groupUrl = WebApiCls.baseUrl + '/group';
    static adminUrl = WebApiCls.baseUrl + '/admin';
    static validateUrl = WebApiCls.baseUrl + '/validate';
    static structureUrl = WebApiCls.baseUrl + '/structure';

    findInFundTree(versionId: number, nodeId: number, searchText: string, type, searchParams = null, luceneQuery = false) {
        const data = {
            versionId: versionId,
            nodeId: nodeId,
            searchValue: searchText,
            depth: type,
            searchParams: searchParams,
            luceneQuery: luceneQuery,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fulltext', null, data);
    }

    getDaDaoListByAipId(id: number) {
        return AjaxUtils.ajaxGet(WebApiCls.v1 + "/daos/aip/" + id);
    }

    getDaoViewRequestInfo(id: number) {
        return AjaxUtils.ajaxGet(WebApiCls.v1 + "/daos/component/" + id);
    }

    syncDaoLink(fundVersionId: number, nodeId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/daos/' + fundVersionId + '/nodes/' + nodeId + '/sync');
    }

    syncDaosByFund(fundVersionId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/daos/' + fundVersionId + '/all/sync');
    }

    /**
     * Seznam AS serazeny podle poctu vyhledanych JP.
     * Vysledek vyhledavani je ulozeny v user session pro pouziti v {@link #fundFulltext(number)}.
     *
     * @param input vstupni data pro fultextove vyhledavani
     * @return seznam AS razeny podle poctu vyhledanych JP
     */
    fundFulltext(filterText) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundFulltext', null, filterText);
    }

    /**
     * Seznam uzlu daneho AS serazeny podle relevance pri vyhledani.

     * @param fundId identifikátor AS
     * @return seznam uzlu daneho AS serazeny podle relevance pri vyhledani
     */
    fundFulltextNodes(fundId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + `/fundFulltext/${fundId}`, { fundId });
    }

    getFundsByVersionIds(versionIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getVersions', null, { ids: versionIds });
    }

    getNode(fundVersionId: number, nodeId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodeInfo/' + fundVersionId + '/' + nodeId);
    }

    getNodes(versionId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodes', null, { versionId: versionId, ids: nodeIds });
    }

    findNodeByIds(fundId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.adminUrl + '/' + fundId + '/nodes/byIds', null, nodeIds);
    }

    copyOlderSiblingAttribute(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/copyOlderSiblingAttribute',
            { versionId, descItemTypeId },
            { id: nodeId, version: nodeVersionId },
        );
    }

    findChanges(versionId: number, nodeId: number, offset, maxSize, changeId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId, { nodeId, offset, maxSize, changeId });
    }

    findChangesByDate(versionId: number, nodeId: number, changeId: number, fromDate) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + '/date', {
            nodeId,
            maxSize: 1,
            changeId,
            fromDate,
        });
    }

    revertChanges(versionId: number, nodeId: number, fromChangeId: number, toChangeId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + '/revert', {
            nodeId,
            fromChangeId,
            toChangeId,
        });
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet(WebApiCls.validateUrl + '/unitDate', { value: value || '' });
    }

    moveNodesUnder(versionId: number, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelUnder', null, data);
    }

    moveNodesBefore(versionId: number, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelBefore', null, data);
    }

    moveNodesAfter(versionId: number, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelAfter', null, data);
    }

    createDescItem(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number, descItem) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
            '/descItems/' +
            versionId +
            '/' +
            nodeId +
            '/' +
            nodeVersionId +
            '/' +
            descItemTypeId +
            '/create',
            null,
            descItem,
        );
    }

    updateDescItem(versionId: number, nodeId: number, nodeVersionId: number, descItem) {
        return callWS(
            '/arrangement/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/update/true',
            descItem,
        );

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/update/true', null,  descItem);
    }

    updateDescItems(
        fundVersionId: number,
        nodeId: number,
        nodeVersionId: number,
        createDescItem = [],
        updateDescItem = [],
        deleteDescItem = [],
    ) {
        const changeItems: any[] = [];

        createDescItem.forEach(item => {
            changeItems.push({
                updateOp: 'CREATE',
                item: item,
            });
        });

        updateDescItem.forEach(item => {
            changeItems.push({
                updateOp: 'UPDATE',
                item: item,
            });
        });

        deleteDescItem.forEach(item => {
            changeItems.push({
                updateOp: 'DELETE',
                item: item,
            });
        });

        return callWS(
            '/arrangement/descItems/' + fundVersionId + '/' + nodeId + '/' + nodeVersionId + '/update/bulk',
            changeItems,
            true,
        );
    }

    inhibitDescItem(nodeId:number, descItemObjectId:number){
        return callWS('/arrangement/descItems/inhibit', {nodeId, descItemObjectId});
    }

    allowDescItem(nodeId: number, descItemObjectId:number){
        return callWS('/arrangement/descItems/allow', {nodeId, descItemObjectId});
    }

    setNotIdentifiedDescItem(versionId: number, nodeId: number, parentNodeVersion, descItemTypeId: number, descItemSpecId: number, descItemObjectId: number) {
        // return callWS('/arrangement/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/set?descItemTypeId=' + descItemTypeId + '&descItemSpecId=' + descItemSpecId + '&descItemObjectId=' + descItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
            '/descItems/' +
            versionId +
            '/' +
            nodeId +
            '/' +
            parentNodeVersion +
            '/notUndefined/set',
            { descItemTypeId, descItemSpecId, descItemObjectId },
        );
    }

    unsetNotIdentifiedDescItem(versionId: number, nodeId: number, parentNodeVersion, descItemTypeId: number, descItemSpecId: number, descItemObjectId: number) {
        // return callWS('/arrangement/descItems/' + versionId + '/' + nodeId + '/' + parentNodeVersion + '/notUndefined/unset?descItemTypeId=' + descItemTypeId + '&descItemSpecId=' + descItemSpecId + '&descItemObjectId=' + descItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
            '/descItems/' +
            versionId +
            '/' +
            nodeId +
            '/' +
            parentNodeVersion +
            '/notUndefined/unset',
            { descItemTypeId, descItemSpecId, descItemObjectId },
        );
    }

    deleteDescItem(versionId: number, nodeId: number, nodeVersionId: number, descItem) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/delete',
            null,
            descItem,
        );
    }

    deleteDescItemType(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number) {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl +
            '/descItems/' +
            versionId +
            '/' +
            nodeId +
            '/' +
            nodeVersionId +
            '/' +
            descItemTypeId,
            null,
            null,
        );
    }

    switchOutputCalculating(fundVersionId: number, getOutputId: number, itemTypeId: number, strict) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + getOutputId + '/' + fundVersionId + '/' + itemTypeId + '/switch',
            { strict },
            null,
        );
    }

    updateOutputSettings(outputId: number, outputSettings) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + '/settings', null, {
            ...outputSettings,
        });
    }

    /**
     * Přidání omezujícího rejstříku k výstupu
     *
     * @param outputId identifikátor výstupu
     * @param templateId identifikátor rejstříku
     */
    addOutputTemplate(outputId: number, templateId: number): Promise<void> {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + '/template/' + templateId, null, null);
    }

    /**
     * Odebrání omezujícího rejstříku z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param templateId identifikátor rejstříku
     */
    deleteOutputTemplate(outputId: number, templateId: number): Promise<void> {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl + '/output/' + outputId + '/template/' + templateId,
            null,
            null,
        );
    }

    /**
     * Přidání omezujícího rejstříku k výstupu
     *
     * @param outputId identifikátor výstupu
     * @param scopeId identifikátor rejstříku
     */
    addRestrictedScope(outputId: number, scopeId: number) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + '/restrict/' + scopeId, null, null);
    }

    /**
     * Odebrání omezujícího rejstříku z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param scopeId identifikátor rejstříku
     */
    deleteRestrictedScope(outputId: number, scopeId: number) {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl + '/output/' + outputId + '/restrict/' + scopeId,
            null,
            null,
        );
    }

    addNode(node, parentNode, versionId: number, direction: number, descItemCopyTypes, scenarioName, createItems, count = 1) {
        const data = {
            versionId,
            direction,
            staticNodeParent: parentNode,
            staticNode: node,
            descItemCopyTypes,
            scenarioName,
            createItems,
            count,
        };

        return callWS('/arrangement/levels/add', data);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/levels', null, data);
    }

    copyNodesValidate(
        targetFundVersionId: number,
        sourceFundVersionId: number,
        sourceNodes,
        ignoreRootNodes = false,
        selectedDirection,
    ) {
        const data = {
            targetFundVersionId,
            sourceFundVersionId,
            sourceNodes,
            ignoreRootNodes,
            selectedDirection,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/levels/copy/validate', null, data);
    }

    copyNodes(
        targetFundVersionId: number,
        targetStaticNode,
        targetStaticNodeParent,
        sourceFundVersionId: number,
        sourceNodes,
        ignoreRootNodes = false,
        selectedDirection,
        filesConflictResolve = null,
        structuresConflictResolve = null,
        templateId: number | null = null,
    ) {
        const data = {
            targetFundVersionId,
            targetStaticNode,
            targetStaticNodeParent,
            sourceFundVersionId,
            sourceNodes,
            ignoreRootNodes,
            selectedDirection,
            filesConflictResolve,
            structuresConflictResolve,
            templateId,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/levels/copy', null, data);
    }

    deleteNode(node, parentNode, version) {
        const data = {
            versionId: version,
            staticNodeParent: parentNode,
            staticNode: node,
        };

        return callWS('/arrangement/levels/delete', data);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxDelete(WebApi.arrangementUrl + '/levels', null, data);
    }

    getNodeAddScenarios(node, versionId: number, direction: number, withGroups = false) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/scenarios',
            { withGroups: withGroups },
            {
                versionId,
                direction,
                node,
            },
        );
    }

    getBulkActions(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/' + versionId, null);
    }

    getBulkActionsState(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/states/' + versionId, null);
    }

    getBulkActionsList(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/list/' + versionId, null);
    }

    bulkActionValidate(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/validate/' + versionId, null);
    }

    getBulkAction(bulkActionRunId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId, null);
    }

    interruptBulkAction(bulkActionRunId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId + '/interrupt', null);
    }

    queueBulkAction(versionId: number, code: string) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId: number, code: string, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null, nodeIds);
    }

    queuePersistentSortByIds(versionId: number, code: string, nodeIds: number[], config) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/persistentSort/' + versionId + '/' + code, null, {
            nodeIds,
            ...config,
        });
    }

    versionValidate(versionId: number, showAll = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersion/' + versionId + '/' + showAll, null);
    }

    versionValidateCount(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersionCount/' + versionId, null);
    }

    getFundPolicy(fundVersionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/fund/policy/' + fundVersionId, {});
    }

    resetServerCache() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/cache/reset', {});
    }

    /// Registry
    createAccessPoint(accessPoint: ApAccessPointCreateVO): Promise<ApAccessPointVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/', null, accessPoint);
    }

    getAccessPoints(text: String, max: number = DEFAULT_LIST_SIZE, from: number = 0): Promise<ApAccessPointVO[]> {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + "/ap",
            {from: from, count: max, text: text},
        );
    }

    getStateApproval(accessPointId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/nextStates');
    }

    getStateApprovalRevision(accessPointId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/nextStatesRevision');
    }

    findAccessPoint(
        search = null,
        registryParent = null,
        apTypeId = null,
        versionId = null,
        itemTypeId = null,
        itemSpecId = null,
        from = 0,
        count = DEFAULT_LIST_SIZE,
        scopeId = null,
        excludeInvalid = true,
        state = null,
        searchTypeName?: ApSearchType,
        searchTypeUsername?: ApSearchType,
        revState = null,
        searchFilter?: ApAdvanceSearchFilter,
    ): Promise<FilteredResultVO<ApAccessPointVO>> {
        // parentRecordId / excludeInvalid are not honored by the new /accesspoint/search endpoint
        // (they were unused legacy parameters that never reached the backend filter pipeline).
        return Api.accesspoints
            .accessPointSearch({
                search: search ?? undefined,
                from: from ?? undefined,
                count: count ?? undefined,
                apTypeId: apTypeId ?? undefined,
                versionId: versionId ?? undefined,
                itemTypeId: itemTypeId ?? undefined,
                itemSpecId: itemSpecId ?? undefined,
                scopeId: scopeId ?? undefined,
                state: state ?? undefined,
                revState: revState ?? undefined,
                searchTypeName: searchTypeName as any,
                searchTypeUsername: searchTypeUsername as any,
                searchFilter: searchFilter as any,
            })
            .then(resp => ({ count: resp.data.count, rows: resp.data.rows ?? [] } as FilteredResultVO<ApAccessPointVO>));
    }

    /**
     * Vyhledání přístupových bodů pro návazný vztah
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param itemTypeId identifikátor typu vztahu
     * @param itemSpecId identifikátor specifikace vztahu
     * @param scopeId oblast hledání
     * @param filter parametry hledání
     * @return výsledek hledání
     *
     */
    findAccessPointForRel(
        from: number,
        max: number,
        itemTypeId: number,
        itemSpecId: number,
        filter: ApAdvanceSearchFilter,
        scopeId?: number,
    ): Promise<ArchiveEntityResultListVO> {
        return AjaxUtils.ajaxPost(
            WebApiCls.registryUrl + '/search/rel',
            {
                from,
                max,
                itemTypeId,
                itemSpecId,
                scopeId,
            },
            filter,
        );
    }

    /**
     * Vyhledání archivních entit v externím systému
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param externalSystemCode kód externího systému
     * @param filter parametry hledání
     * @return výsledek hledání
     */
    findArchiveEntitiesInExternalSystem(
        from: number,
        max: number,
        externalSystemCode: string,
        filter: ApAdvanceSearchFilter,
    ): Promise<ArchiveEntityResultListVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/external/search', { from, max, externalSystemCode }, filter);
    }

    /**
     * Vyhledání položek ve frontě na synchronizaci.
     *
     * @param from od které položky vyhledávat
     * @param max maximální počet záznamů, které najednou vrátit
     * @param externalSystemCode kód externího systému
     * @param filter parametry hledání
     * @return výsledek hledání
     */
    findExternalSyncs(
        from: number,
        max: number,
        externalSystemCode: string,
        filter: SyncsFilterVO,
    ): Promise<ExtSyncsQueueResultListVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/external/syncs', { from, max, externalSystemCode }, filter);
    }

    /**
     * Převzetí entity z externího systému
     *
     * @param archiveEntityId identifikátor entity v externím systému
     * @param scopeId identifikátor třídy rejstříku
     * @param externalSystemCode kód externího systému
     * @return identifikátor přístupového bodu
     */
    takeArchiveEntity(archiveEntityId: number, scopeId: number, externalSystemCode: string): Promise<number> {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/{archiveEntityId}/take', {
            archiveEntityId,
        });
        return AjaxUtils.ajaxPost(url, { scopeId, externalSystemCode });
    }

    /**
     * Propojení archivní entity z externího systém na existující přístupový bod
     *
     * @param archiveEntityId identifikátor entity v externím systému
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     * @param replace nahradit původní data přístupového bod (převezme se kompletně z ext. systému)
     */
    connectArchiveEntity(
        archiveEntityId: number,
        accessPointId: number,
        externalSystemCode: string,
        replace: boolean,
    ): Promise<void> {
        const url = UrlBuilder.bindParams(
            WebApiCls.registryUrl + '/external/{archiveEntityId}/connect/{accessPointId}',
            {
                archiveEntityId,
                accessPointId,
            },
        );
        return AjaxUtils.ajaxPost(url, { externalSystemCode, replace });
    }

    /**
     * Zápis přistupového bodu do externího systému
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    saveAccessPoint(accessPointId: number, externalSystemCode: string): Promise<void> {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/save/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, { externalSystemCode });
    }

    /**
     * Synchronizace přístupového bodu z externího systému
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    synchronizeAccessPoint(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/synchronize/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, { externalSystemCode });
    }

    /**
     * Zápis změn do externího systému
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param externalSystemCode kód externího systému
     */
    updateArchiveEntity(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/update/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, { externalSystemCode });
    }

    disconnectAccessPoint(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/disconnect/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, { externalSystemCode });
    }

    takeRelArchiveEntities(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/take-rel/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, { externalSystemCode });
    }

    getApTypeViewSettings(): Promise<ApViewSettings> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/ap-types/view-settings');
    }

    findRegistryUsage(recordId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + recordId + '/usage');
    }

    getAccessPoint(accessPointId): Promise<ApAccessPointVO> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId);
    }

    findStateHistories(accessPointId: number): Promise<ApStateHistoryVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/history');
    }

    updateAccessPoint(accessPointId: number, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId, null, data);
    }

    changeDescription(accessPointId: number, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/description', null, data);
    }

    replaceRegistry(recordReplaceId: number, recordReplacementId: number) {
        return AjaxUtils.ajaxPost(
            WebApiCls.registryUrl + '/' + recordReplaceId + '/replace',
            null,
            recordReplacementId,
        );
    }

    getScopes(versionId: number | null = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/fundScopes', { versionId });
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/scopes', null);
    }

    getScopeWithConnected(scopeId: number | null = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/scopes/' + scopeId + '/withConnected', null);
    }

    createScope() {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/scopes', null);
    }

    updateScope(scopeId: number, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/scopes/' + scopeId, null, data);
    }

    deleteScope(scopeId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/scopes/' + scopeId, null);
    }

    connectScope(scopeId: number, connectedScopeId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/scopes/' + scopeId + '/connect', null, connectedScopeId);
    }

    disconnectScope(scopeId: number, connectedScopeId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/scopes/' + scopeId + '/disconnect', null, connectedScopeId);
    }

    getAllLanguages() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/languages', null);
    }

    getRecordTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/recordTypes');
    }

    /**
     * Založení nové části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param apPartFormVO data pro vytvoření části
     * @param apVersion verze přístupového bodu
     * @return poartId,apVersion
     */
    createPart(accessPointId: number, apPartFormVO: ApPartFormVO, apVersion?: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.accesspointUrl + '/' + accessPointId + '/part', { apVersion }, apPartFormVO);
    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor upravované části
     * @param apPartFormVO data pro úpravu části
     * @param apVersion verze přístupového bodu
     * @return apVersion
     */
    updatePart(accessPointId: number, partId: number, apPartFormVO: ApPartFormVO, apVersion?: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.accesspointUrl + '/' + accessPointId + '/part/' + partId, { apVersion }, apPartFormVO);
    }

    /**
     * Úprava části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor upravované části
     * @param apPartFormVO data pro úpravu části
     * @param apVersion verze přístupového bodu
     * @return apVersion
     */
    updateRevisionPart(accessPointId: number, partId: number, apPartFormVO: ApPartFormVO, apVersion?: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/revision/part/' + partId, { apVersion }, apPartFormVO);
    }

    /**
     * Smazání části přístupového bodu.
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @param partId identifikátor mazané části
     * @deprecated next use Api.accesspoints.accessPointDeletePart()
     */
    deletePart(accessPointId: number, partId: number): Promise<void> {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/' + accessPointId + '/part/' + partId, null, null);
    }

    /**
     * Zjištění povinných a možných atributů pro zakládání nového přístupového bodu nebo nové části
     *
     * @param apAccessPointCreateVO průběžná data pro založení
     * @return vyhodnocené typy a specifikace atributů, které jsou třeba pro založení přístupového bodu nebo části
     */
    getAvailableItems(apAccessPointCreateVO: ApAccessPointCreateVO): Promise<ApAttributesInfoVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/available/items', null, apAccessPointCreateVO);
    }

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    getApTypes(): Promise<ApTypeVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/recordTypes');
    }

    // End registry

    getFundNodeForm(versionId: number, nodeId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeForms(versionId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + versionId + '/forms', { nodeIds: nodeIds });
    }

    getFundNodeFormsWithAround(versionId: number, nodeId: number, around) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms',
        );
    }

    getFundNodeRegister(versionId: number, nodeId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeDaos(versionId: number, nodeId: number | null = null, detail = false, from = 0, max = 10000) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daos/' + versionId, {
            nodeId,
            detail,
            index: from,
            maxResults: max,
        });
    }

    findDaoPackages(versionId: number, search: string, unassigned) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daopackages/' + versionId, { search, unassigned });
    }

    getPackageDaos(versionId: number, daoPackageId: number, unassigned, detail = false, from = 0, max = 10000) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daos/' + versionId + '/' + daoPackageId, {
            detail,
            index: from,
            maxResults: max,
            unassigned,
        });
    }

    getRulDataTypes(): Promise<RulDataTypeVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/dataTypes');
    }

    getDescItemTypes(): Promise<RulDescItemTypeExtVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/descItemTypes');
    }

    getGroups(fundVersionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/groups/' + fundVersionId);
    }

    getTemplates(code: string | null = null) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/templates', code ? { code } : null);
    }

    getRequestsInQueue() {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/queued');
    }

    findRequests(versionId: number, type, state, description: string, fromDate, toDate, subType) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId, {
            state,
            type,
            description,
            fromDate,
            toDate,
            subType,
        });
    }

    arrDigitizationRequestAddNodes(versionId: number, reqId: number, send, description: string, nodeIds: number[], digitizationFrontdeskId: number) {
        const data = {
            id: reqId,
            nodeIds,
            description,
            digitizationFrontdeskId,
        };
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/requests/' + versionId + '/digitization/add',
            { send },
            data,
        );
    }

    arrDaoRequestAddDaos(versionId: number, reqId: number, send, description: string, daoIds: number[], type) {
        const data = {
            id: reqId,
            daoIds,
            description,
            type,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/dao/add', { send }, data);
    }

    arrRequestRemoveNodes(versionId: number, reqId: number, nodeIds: number[]) {
        const data = {
            id: reqId,
            nodeIds,
        };
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/requests/' + versionId + '/digitization/remove',
            null,
            data,
        );
    }

    updateArrRequest(versionId: number, id: number, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id, null, data);
    }

    removeArrRequestQueueItem(id: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getArrRequest(versionId: number, id: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id, { detail: true });
    }

    sendArrRequest(versionId: number, id: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id + '/send');
    }

    deleteArrRequest(versionId: number, id: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getFundTree(versionId: number, nodeId: number, expandedIds: Record<string, boolean> = {}, includeIds: number[] = []) {
        const data = {
            versionId,
            nodeId,
            includeIds,
            expandedIds: expandedIds ? Object.keys(expandedIds) : [],
        };

        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundTree', null, data);
    }

    getAipsLogicalTree(aipIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/levelViewTree', null, aipIds);
    }

    aipDeleteDaoLink(daoLinkId: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + `/aip/delete-dao-link?daoLinkId=${daoLinkId}`);
    }

    connectAipToJp(arrNodeId: number, daAipId: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/connect-to-jp', {
            arrNodeId: arrNodeId,
            daAipId: daAipId
        }, null);
    }

    connectAipPartToJp(arrNodeId: number, daAipId: number, daDaoIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/connect-part-to-jp', {
            arrNodeId: arrNodeId,
            daAipId: daAipId,
            daDaoIdList: daDaoIdList
        }, null);
    }

    createJpFromSelectedAip(arrNodeId: number, daAipId: number, daDaoIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/connect-jp-from-selected', {
            arrNodeId: arrNodeId,
            daAipId: daAipId,
            daDaoIdList: daDaoIdList
        }, null);
    }

    createJpLinkFromSelectedAip(arrNodeId: number, daAipId: number, daDaoIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/connect-jp-link-from-selected', {
            arrNodeId: arrNodeId,
            daAipId: daAipId,
            daDaoIdList: daDaoIdList
        }, null);
    }

    connectSelectedToJp(arrNodeId: number, daAipId: number, daDaoIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/connect-selected-to-jp', {
            arrNodeId: arrNodeId,
            daAipId: daAipId,
            daDaoIdList: daDaoIdList
        }, null);
    }

    connectSelectedAipToJp(arrNodeId: number, daAipIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/bulk-connect-to-jp', {
            arrNodeId: arrNodeId,
            daAipIdList: daAipIdList
        }, null);
    }

    createJpFromSelectedAipBulk(arrNodeId: number, daAipIdList: number[]): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/bulk-create-from-selected', {
            arrNodeId: arrNodeId,
            daAipIdList: daAipIdList
        }, null);
    }

    createJpFromSelectedAipAnConnectBulk(arrNodeId: number, aipIds: number[], daLevelViewId: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/bulk-create-selected-to-jp', {
            arrNodeId: arrNodeId,
            daAipIdList: aipIds,
            daLevelViewId: daLevelViewId
        }, null);
    }

    connectAipLogicalStructureToJpBulk(arrNodeId: number, aipIds: number[], daLevelViewId: number): Promise<void> {
        return AjaxUtils.ajaxPost(WebApiCls.aipV1 + '/bulk-connect-logic-to-jp', {
            arrNodeId: arrNodeId,
            daAipIdList: aipIds,
            daLevelViewId: daLevelViewId
        }, null);
    }

    getFundTreeNodes(versionId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundTree/nodes', null, {
            versionId,
            nodeIds,
        });
    }

    getExternalSystemsSimple() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems/simple');
    }

    getAsyncRequestInfo() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/asyncRequests');
    }

    getAsyncRequestDetail(requestType) {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/asyncRequests/' + requestType);
    }

    getLogs(lineCount, firstLine) {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/logs', { lineCount, firstLine });
    }

    getRuleSets() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/getRuleSets');
    }

    findExportFilters() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/exportFilters');
    }

    findOutputFilters() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/outputFilters');
    }

    approveVersion(versionId: number) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/approveVersion', { versionId });
    }

    filterNodes(versionId: number, filter) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/filterNodes/' + versionId, {}, filter);
    }

    getFilteredNodes(versionId: number, pageIndex: number, pageSize: number, descItemTypeIds: number[]) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/getFilterNodes/' + versionId,
            { page: pageIndex, pageSize: pageSize },
            descItemTypeIds,
        );
    }

    getFilteredNodes2(versionId: number, pageIndex: number, pageSize: number, descItemTypeIds: number[]) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/getFilterNodes2/' + versionId,
            { page: pageIndex, pageSize: pageSize },
            descItemTypeIds,
        );
    }

    getFilteredNodesByNodeId(versionId: number, nodeId: number, pageSize: number, descItemTypeIds: number[]) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/getFilterNodes2/' + versionId,
            { nodeId, pageSize },
            descItemTypeIds,
        );
    }

    replaceDataValues(versionId: number, descItemTypeId: number, specsIds: number[], searchText: string, replaceText: string, nodes, selectionType) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/replaceDataValues/' + versionId,
            { descItemTypeId, searchText, replaceText },
            { nodes, specIds: specsIds, selectionType },
        );
    }

    placeDataValues(versionId: number, descItemTypeId: number, specsIds: number[], replaceText: string, description: string, replaceSpecId: number, nodes, selectionType, append = false) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/placeDataValues/' + versionId + (append ? '?append=true' : ""),
            { descItemTypeId, newDescItemSpecId: replaceSpecId, text: replaceText, description },
            { nodes, specIds: specsIds, selectionType },
        );
    }

    setSpecification(fundVersionId: number, itemTypeId: number, specIds: number[], replaceSpecId: number, nodes, selectionType) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/setSpecification/' + fundVersionId,
            { itemTypeId, replaceSpecId },
            { nodes, specIds, selectionType },
        );
    }

    setDataValues(fundVersionId: number, itemTypeId: number, specIds: number[], replaceValueId: number, nodes, selectionType, valueIds: number[]) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/setDataValues/' + fundVersionId,
            { itemTypeId, replaceValueId },
            { nodes, specIds, selectionType, valueIds },
        );
    }

    deleteDataValues(versionId: number, descItemTypeId: number, specsIds: number[], nodes, selectionType, valueIds: number[]) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/deleteDataValues/' + versionId,
            { descItemTypeId },
            { nodes, specIds: specsIds, selectionType, valueIds },
        );
    }

    getFilteredFulltextNodes(versionId: number, fulltext: string, luceneQuery = false, searchParams = null) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getFilteredFulltext/' + versionId, null, {
            fulltext,
            luceneQuery,
            searchParams,
        });
    }

    getPackages() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/getPackages');
    }

    deletePackage(code: string) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/deletePackage/' + code);
    }

    createDaoLink(versionId: number, daoId: number, nodeId: number) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/daos/' + versionId + '/' + daoId + '/' + nodeId + '/create',
            null,
            null,
        );
    }

    deleteDaoLink(versionId: number, daoLinkId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/daolinks/' + versionId + '/' + daoLinkId, null, null);
    }

    /**
     * Získání odkazovaných JP.
     *
     * @param fundVersionId verze AS
     * @param nodeId        JP pro kterou zjišťujeme odkazované JP
     * @return seznam JP
     */
    findLinkedNodes(fundVersionId: number, nodeId: number) {
        const url = UrlBuilder.bindParams(WebApiCls.arrangementUrl + '/nodes/{nodeId}/{fundVersionId}/links', {
            fundVersionId, nodeId
        });

        return AjaxUtils.ajaxGet(url);
    }

    createRefTemplate(fundId: number): Promise<ArrRefTemplateVO> {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/nodes/' + fundId + '/template/create');
    }

    updateRefTemplate(templateId: number, refTemplateVO: ArrRefTemplateEditVO): Promise<ArrRefTemplateVO> {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodes/template/' + templateId, null, refTemplateVO);
    }

    deleteRefTemplate(templateId: number): Promise<void> {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/nodes/template/' + templateId);
    }

    getRefTemplates(fundId: number): Promise<ArrRefTemplateVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + fundId + '/template');
    }

    createRefTemplateMapType(
        templateId: number,
        refTemplateMapTypeFormVO: ArrRefTemplateMapTypeVO,
    ): Promise<ArrRefTemplateMapTypeVO> {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/nodes/template/' + templateId + '/map-type',
            null,
            refTemplateMapTypeFormVO,
        );
    }
    updateRefTemplateMapType(
        templateId: number,
        mapTypeId: number,
        refTemplateMapTypeFormVO: ArrRefTemplateMapTypeVO,
    ): Promise<ArrRefTemplateMapTypeVO> {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/nodes/template/' + templateId + '/map-type/' + mapTypeId,
            null,
            refTemplateMapTypeFormVO,
        );
    }

    deleteRefTemplateMapType(templateId: number, mapTypeId: number): Promise<void> {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl + '/nodes/template/' + templateId + '/map-type/' + mapTypeId,
        );
    }

    synchronizeNodes(nodeId: number, nodeVersion: number, childrenNodes: boolean): Promise<void> {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + nodeId + '/' + nodeVersion + '/sync', {
            childrenNodes,
        });
    }

    importPackage(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.ruleUrl + '/importPackage', {}, 'POST', data);
    }

    reindex() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/reindex');
    }

    getIndexingState() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/reindexStatus');
    }

    getTransformations() {
        return AjaxUtils.ajaxGet(WebApiCls.importUrl + '/transformations');
    }

    getExportTransformations() {
        return AjaxUtils.ajaxGet(WebApiCls.importUrl + '/transformations');
    }

    xmlImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.importUrl + '/import', {}, 'POST', data);
    }

    arrCoordinatesImport(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', String(versionId));
        formData.append('descItemTypeId', String(descItemTypeId));
        formData.append('nodeId', String(nodeId));
        formData.append('nodeVersion', String(nodeVersionId));

        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/descCoordinates', {}, 'POST', formData);
    }

    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/regCoordinates', {}, 'POST', data);
    }

    descItemCsvImport(versionId: number, nodeId: number, nodeVersionId: number, descItemTypeId: number, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('nodeId', String(nodeId));
        formData.append('nodeVersion', String(nodeVersionId));
        formData.append('descItemTypeId', String(descItemTypeId));

        return AjaxUtils.ajaxCallRaw(
            WebApiCls.arrangementUrl + '/descItems/' + versionId + '/csv/import',
            {},
            'POST',
            formData,
        );
    }

    getInstitutions(hasFund = null) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/institutions', { hasFund });
    }

    /**
     * Hledá všechny unikátní hodnoty atributu pro daný AS
     */
    getDescItemTypeValues(versionId: number, descItemTypeId: number, fulltext: string, descItemSpecIds: number[], max) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/filterUniqueValues/' + versionId,
            { descItemTypeId, fulltext, max },
            descItemSpecIds,
        );
    }

    findUniqueSpecIds(fundVersionId: number, itemTypeId: number, filters) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/findUniqueSpecIds/' + fundVersionId,
            { itemTypeId },
            filters,
        );
    }

    getVisiblePolicy(nodeId: number, fundVersionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/types');
    }

    setVisiblePolicy(nodeId: number, fundVersionId: number, policyTypeIdsMap, includeSubtree = false, nodeExtensions) {
        return AjaxUtils.ajaxPut(WebApiCls.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId, null, {
            policyTypeIdsMap,
            includeSubtree,
            nodeExtensions,
        });
    }

    getUserDetail() {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/detail');
    }

    setUserSettings(settings) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/detail/settings', null, settings);
    }

    login(username: string, password) {
        return AjaxUtils.ajaxCallRaw(
            '/login',
            {},
            'POST',
            'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password),
            'application/x-www-form-urlencoded',
        );
    }

    logout() {
        return AjaxUtils.ajaxCallRaw('/logout', {}, 'POST', '', 'application/x-www-form-urlencoded', true);
    }

    /**
     * @deprecated use #{WebApiCls.findFunds2}
     * @param fulltext
     * @param max
     * @param from
     */
    findFunds(fulltext: string, max = DEFAULT_LIST_SIZE, from = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFunds', { fulltext, max, from }).then(json => ({
            funds: json.list,
            fundCount: json.count,
            max,
            from,
        }));
    }

    findUser(
        fulltext: string | null,
        active: boolean,
        disabled: boolean,
        max: number = DEFAULT_LIST_SIZE,
        groupId: number | null = null,
        searchTypeName?: ApSearchType,
        searchTypeUsername?: ApSearchType,
        all?: boolean,
    ): Promise<RowsResponse<UsrUserVO>> {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '', {
            search: fulltext,
            active,
            disabled,
            from: 0,
            count: max,
            excludedGroupId: groupId,
            searchTypeName,
            searchTypeUsername,
            all,
        }).then(json => ({ data: json.rows, count: json.count }));
    }

    findControlFunds(fulltext: string, max: number = DEFAULT_LIST_SIZE, from: number = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/controlFunds', { search: fulltext, from, count: max });
    }

    findUserWithFundCreate(
        fulltext,
        active,
        disabled,
        max = DEFAULT_LIST_SIZE,
        groupId = null,
        searchTypeName?: ApSearchType,
        searchTypeUsername?: ApSearchType,
    ): Promise<RowsResponse<UsrUserVO>> {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/withFundCreate', {
            search: fulltext,
            active,
            disabled,
            from: 0,
            count: max,
            excludedGroupId: groupId,
            searchTypeName,
            searchTypeUsername,
        }).then(json => ({ data: json.rows, count: json.count }));
    }

    findUsersPermissionsByFund(fundId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + `/fund/${fundId}/users`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    findUsersPermissionsByFundAll() {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + `/fund/all/users`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    findGroupsPermissionsByFund(fundId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/${fundId}/groups`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    findGroupsPermissionsByFundAll(fundId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/all/groups`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    changeUserPermission(userId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission', null, permissions);
    }

    addUserPermission(userId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/add', null, permissions);
    }

    addGroupPermission(groupId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/add', null, permissions);
    }

    deleteUserPermission(userId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete', null, permissions);
    }

    deleteGroupPermission(groupId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete', null, permissions);
    }

    deleteUserFundPermission(userId: number, fundId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/fund/' + fundId);
    }

    deleteUserFundAllPermission(userId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/fund/all');
    }

    deleteGroupFundPermission(groupId: number, fundId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/fund/' + fundId);
    }

    deleteGroupFundAllPermission(groupId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/fund/all');
    }

    deleteUserScopePermission(userId: number, scopeId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/scope/' + scopeId);
    }

    deleteGroupScopePermission(groupId: number, scopeId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/scope/' + scopeId);
    }

    changeGroupPermission(groupId: number, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/' + groupId + '/permission', null, permissions);
    }

    findGroup(fulltext: string, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl, { search: fulltext, from: 0, count: max }).then(json => ({
            groups: json.rows,
            groupsCount: json.count,
        }));
    }

    findGroupWithFundCreate(fulltext: string, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + '/withFundCreate', {
            search: fulltext,
            from: 0,
            count: max,
        }).then(json => ({ groups: json.rows, groupsCount: json.count }));
    }

    getUser(userId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId);
    }

    getUserOld(userId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId + '/old');
    }

    createGroup(name: string, code: string, description: string) {
        const params = {
            name: name,
            code: code,
            description,
        };
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl, null, params);
    }

    updateGroup(groupId: number, name: string, description: string) {
        return AjaxUtils.ajaxPut(WebApiCls.groupUrl + '/' + groupId, null, { name, description });
    }

    deleteGroup(groupId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.groupUrl + '/' + groupId);
    }

    joinGroup(groupIds: number[], userIds: number[]) {
        const data = {
            groupIds: groupIds,
            userIds: userIds,
        };
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/join', null, data);
    }

    leaveGroup(groupId: number, userId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/' + groupId + '/leave/' + userId, null, null);
    }

    createUser(username: string, valuesMap, accessPointId: number) {
        const params = {
            username: username,
            valuesMap: valuesMap,
            accessPointId: accessPointId,
        };
        return AjaxUtils.ajaxPost(WebApiCls.userUrl, null, params);
    }

    updateUser(id: number, accessPointId: number, username: string, valuesMap) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + id, null, { accessPointId, username, valuesMap });
    }

    changePasswordUser(oldPassword, newPassword: string) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/password', null, { oldPassword, newPassword });
    }

    changePassword(userId: number, newPassword: string) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/password', null, { newPassword });
    }

    changeActive(userId: number, active: boolean) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/active/' + active);
    }

    getGroup(groupId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + '/' + groupId);
    }

    getFundDetail(fundId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFund/' + fundId).then(json => {
            return {
                ...json,
                versionId: json.versions[0].id,
                activeVersion: json.versions[0],
            };
        });
    }

    getAip(aipId): Promise<AipDetailVO> {
        return AjaxUtils.ajaxGet(WebApiCls.aipV1 + '/' + aipId);
    }


    getValidationItems(fundVersionId: number, fromIndex, toIndex) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/' + fromIndex + '/' + toIndex,
        );
    }

    findValidationError(fundVersionId: number, nodeId: number, direction: number) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/find/' + nodeId + '/' + direction,
        );
    }

    deleteFund(fundId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/deleteFund/' + fundId);
    }

    deleteFundHistory(fundId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/deleteFundHistory/' + fundId);
    }

    getOutputTypes(versionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/types/' + versionId);
    }

    getOutputs(versionId: number, state) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/output/' + versionId + (state != null ? '?state=' + state : ''),
        );
    }

    getFundOutputDetail(versionId: number, outputId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createOutput(versionId: number, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + versionId, null, data);
    }

    updateOutput(versionId: number, outputId: number, data) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/update',
            null,
            data,
        );
    }

    outputUsageEnd(versionId: number, outputId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/lock');
    }

    fundOutputAddNodes(versionId: number, outputId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/add',
            null,
            nodeIds,
        );
    }

    fundOutputRemoveNodes(versionId: number, outputId: number, nodeIds: number[]) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/remove',
            null,
            nodeIds,
        );
    }

    outputDelete(versionId: number, outputId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createFundFileRaw(formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund', {}, 'POST', formData);
    }

    createFundFile(formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund', null, formData);
    }

    getMimeTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.attachmentUrl + '/mimeTypes', null);
    }

    findFundFiles(fundId: number, searchText: string, count = 20, from = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId, { count: count, search: searchText, from });
    }

    getEditableFundFile(fundId: number, fileId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId + '/' + fileId);
    }

    updateFundFileRaw(fileId: number, formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund/' + fileId, {}, 'POST', formData);
    }

    updateFundFile(fileId: number, formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund/' + fileId, null, formData);
    }

    deleteArrFile(fileId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.dmsUrl + '/fund/' + fileId, null, null);
    }

    findFundOutputFiles(outputId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/output/' + outputId);
    }

    getFundOutputFunctions(outputId: number, getRecommended) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/output/' + outputId, { recommended: getRecommended });
    }

    outputGenerate(outputId: number, forced = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/generate/' + outputId, { forced });
    }

    outputSend(outputId: number): Promise<void> {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/send/' + outputId.toString());
    }

    outputRevert(versionId: number, outputId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/revert');
    }

    outputClone(versionId: number, outputId: number) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/clone');
    }

    getApExternalSystems() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/externalSystems');
    }

    getKmlExternalSystems() {
        return AjaxUtils.ajaxGet(WebApiCls.kmlUrl + '/externalSystems');
    }

    getEidTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/eidTypes');
    }

    getAllExtSystem() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems', null);
    }

    getAllDigitalRepositorySystem() {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/digitalRepositories', null);
    }

    getExtSystem(id: number) {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems/' + id, null);
    }

    createExtSystem(extSystem) {
        return AjaxUtils.ajaxPost(WebApiCls.adminUrl + '/externalSystems', null, extSystem);
    }

    updateExtSystem(id: number, extSystem) {
        return AjaxUtils.ajaxPut(WebApiCls.adminUrl + '/externalSystems/' + id, null, extSystem);
    }

    deleteExtSystem(id: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.adminUrl + '/externalSystems/' + id, null);
    }

    deleteExtSyncsQueueItem(itemId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/external/syncs/' + itemId, null);
    }

    findFundStructureExtension(fundVersionId: number, structureTypeCode: string) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode);
    }

    updateFundStructureExtension(fundVersionId: number, structureTypeCode: string, structureExtensionCodes) {
        return AjaxUtils.ajaxPut(
            WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode,
            null,
            structureExtensionCodes,
        );
    }

    findRulStructureTypes(fundVersionId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/type', { fundVersionId });
    }

    getStructureData(fundVersionId: number, structureDataId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    findStructureData(
        fundVersionId: number,
        structureTypeCode,
        search: string | null = null,
        assignable: boolean = true,
        from: number = 0,
        count: number = DEFAULT_LIST_SIZE,
    ) {
        return AjaxUtils.ajaxGet(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureTypeCode + '/search',
            {
                search,
                assignable,
                from,
                count,
            },
        );
    }

    createStructureData(fundVersionId: number, structureTypeCode: string, value = null) {
        // Kvůli JSON stringify musíme poslat pomocí RAW aby se nevytvořili '"' v body
        return AjaxUtils.ajaxCallRaw(
            WebApiCls.structureUrl + '/data/' + fundVersionId,
            { value },
            'POST',
            structureTypeCode,
            'application/json',
        );
    }

    duplicateStructureDataBatch(fundVersionId: number, structureDataId: number, data) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/batch',
            null,
            data,
        );
    }

    confirmStructureData(fundVersionId: number, structureDataId: number) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/confirm',
        );
    }

    deleteStructureData(fundVersionId: number, structureDataId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    updateStructureDataBatch(fundVersionId: number, structureTypeCode: string, structureDataBatchUpdate) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureTypeCode + '/batchUpdate',
            null,
            structureDataBatchUpdate,
        );
    }

    setAssignableStructureDataList(fundVersionId: number, assignable, structureDataIds: number[]) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/assignable/' + assignable,
            null,
            structureDataIds,
        );
    }

    getItemTypeCodesByRuleSet(ruleSetCode) {
        return AjaxUtils.ajaxGet(
            WebApiCls.ruleUrl + '/itemTypeCodes/' + ruleSetCode);
    }

    /**
     * Získání druhů připomnek.
     *
     * @returns {Promise} list druhů připomínek
     */
    findAllIssueTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_types');
    }

    /**
     * Získání stavů připomínek.
     *
     * @returns {Promise} list stavů připomínek
     */
    findAllIssueStates(): Promise<IssueStateVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_states');
    }

    /**
     * Získání protokolů pro konkrétní archivní souboru.
     *
     * @param fundId identifikátor AS
     * @param open filter zda je issue list otevřen nebo zavřen
     * @returns {Promise} seznam protokolů
     */
    findIssueListByFund(fundId: number, open: boolean | null = null) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/funds/' + fundId + '/issue_lists', { open });
    }

    /**
     * Získání detailu protokolu.
     *
     * @param issueListId identifikátor protokolu.
     * @returns {Promise} detail protokolu
     */
    getIssueList(issueListId: number): Promise<IssueListVO> {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_lists/' + issueListId);
    }

    /**
     * Získání seznam připomínek dle parametrů.
     *
     * @param issueListId identifikátor protokolu.
     * @param issueStateId identifikátor stavu připomínky dle kterého filtrujeme
     * @param issueTypeId identifikátor druhu připomínky dle kterého filtrujeme
     * @returns {Promise} seznam připomínek
     */
    findIssueByIssueList(issueListId: number, issueStateId: number | null = null, issueTypeId: number | null = null) {
        const requestParams = {
            issueStateId,
            issueTypeId,
        };
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issue_lists/' + issueListId + '/issues', requestParams);
    }

    /**
     * Založení nového protokolu.
     *
     * @param data {IssueListVO} data pro založení protokolu
     */
    addIssueList(data: IssueListVO): Promise<IssueListVO> {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issue_lists', null, data);
    }

    /**
     * Úprava vlastností existujícího protokolu
     *
     * @param issueListId identifikátor protokolu.
     * @param data {IssueListVO} data pro uložení protokolu
     */
    updateIssueList(issueListId: number, data: IssueListVO): Promise<IssueListVO> {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/issue_lists/' + issueListId, null, data);
    }

    /**
     * Odebrání existujícího protokolu
     *
     * @param issueListId identifikátor protokolu
     */
    deleteIssueList(issueListId: number) {
        return AjaxUtils.ajaxDelete(WebApiCls.issueUrl + '/issue_lists/' + issueListId);
    }

    /**
     * Získání detailu připomínky.
     *
     * @param issueId identifikátor připomínky
     * @returns {Promise} detail připomínky
     */
    getIssue(issueId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issues/' + issueId);
    }

    /**
     * Přidání připomínky k protokolu.
     *
     * @param data {IssueVO} data pro přidání připomínky
     * @returns {Promise}
     */
    addIssue(data: IssueVO) {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issues', null, data);
    }

    /**
     * Úprava připomínky.
     *
     * @param issueId identifikátor připomínky
     * @param data {IssueVO} data pro uložení připomínky
     */
    updateIssue(issueId: number, data: IssueVO) {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/issues/' + issueId, null, data);
    }

    /**
     * Změna druhu připomínky.
     *
     * @param issueId     identifikátor připomínky
     * @param issueTypeId identifikátor stavu připomínky
     * @returns {Promise}
     */
    setIssueType(issueId: number, issueTypeId: number) {
        const requestParams = {
            issueTypeId,
        };
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/issues/' + issueId + '/type', requestParams);
    }

    /**
     * Vyhledání komentářů k připomínce.
     *
     * @param issueId identifikátor připomínky
     * @returns {Promise} pole {CommentVO}
     */
    findIssueCommentByIssue(issueId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/issues/' + issueId + '/comments');
    }

    /**
     * Získání detailu komentáře.
     *
     * @param commentId identifikátor komentáře
     * @returns {Promise} detail {CommentVO}
     */
    getIssueComment(commentId: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/comments/' + commentId);
    }

    /**
     * Založení nového komentáře.
     *
     * @param data komentář
     * @returns {Promise}
     */
    addIssueComment(data: Partial<CommentVO>) {
        return AjaxUtils.ajaxPost(WebApiCls.issueUrl + '/comments', null, data);
    }

    /**
     * Úprava komentáře.
     *
     * @param commentId identifikátor komentáře
     * @param data komentář
     * @returns {Promise}
     */
    updateIssueComment(commentId: number, data: CommentVO) {
        return AjaxUtils.ajaxPut(WebApiCls.issueUrl + '/comments/' + commentId, null, data);
    }

    /**
     * Vyhledá další uzel s otevřenou připomínkou.
     *
     * @param fundVersionId verze AS
     * @param nodeId výchozí uzel (default root)
     * @param direction krok (default 1)
     */
    nextIssueByFundVersion(fundVersionId: number, nodeId: number, direction: number) {
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/funds/' + fundVersionId + '/issues/nextNode', {
            nodeId,
            direction,
        });
    }

    importApCoordinates(body: ArrayBuffer | Blob | string, fileType: CoordinateFileType = CoordinateFileType.KML) {
        return AjaxUtils.ajaxCallRaw(
            WebApiCls.apUrl + '/import/coordinates',
            {
                fileType,
            },
            "POST",
            body,
        );
    }

    mapLayerConfiguration(): Promise<MapLayerVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/layer/configuration');
    }
}

/**
 * Továrna URL
 *
 * Jednoduché statické metody vracející pouze String - URL
 */
export class UrlFactory {
    static exportPackage(code) {
        return serverContextPath + WebApiCls.ruleUrl + '/exportPackage/' + code;
    }

    static exportFund() {
        return serverContextPath + WebApiCls.exportUrl + '/create';
    }

    /* Export data from grid */
    static exportGridData(versionId, exportType, columns) {
        return (
            serverContextPath +
            WebApiCls.arrangementUrl +
            '/dataGrid/export/' +
            versionId +
            '/' +
            exportType +
            '?rulItemTypeIds=' +
            columns
        );
    }
    static exportRegCoordinate(objectId) {
        return serverContextPath + WebApiCls.kmlUrl + '/export/regCoordinates/' + objectId;
    }

    static exportArrCoordinate(objectId, versionId) {
        return serverContextPath + WebApiCls.kmlUrl + '/export/descCoordinates/' + versionId + '/' + objectId;
    }

    static exportApCoordinate(itemId, fileType: CoordinateFileType = CoordinateFileType.KML) {
        return (
            UrlBuilder.bindParams(WebApiCls.apUrl + '/export/coordinates/{itemId}', {
                itemId,
            }) +
            '?fileType=' +
            fileType
        );
    }

    static exportArrCoordinates(itemId, fileType: CoordinateFileType = CoordinateFileType.KML) {
        return (
            UrlBuilder.bindParams(WebApiCls.arrangementUrl + '/export/coordinates/{itemId}', {
                itemId,
            }) +
            '?fileType=' +
            fileType
        );
    }

    static exportItemCsvExport(objectId, versionId, typePrefix) {
        return (
            serverContextPath +
            WebApiCls.arrangementUrl +
            '/' +
            typePrefix +
            'Items/' +
            versionId +
            '/csv/export?descItemObjectId=' +
            objectId
        );
    }

    static downloadDmsFile(id) {
        return serverContextPath + WebApiCls.dmsUrl + '/' + id;
    }

    static downloadGeneratedDmsFile(id, fundId, mimeType) {
        return serverContextPath + WebApiCls.dmsUrl + `/fund/${fundId}/${id}/generated?mimeType=${mimeType}`;
    }

    static downloadOutputResult(id) {
        return serverContextPath + '/api/outputResult/' + id;
    }

    static downloadOutputResults(outputId: number) {
        return `${serverContextPath}/api/outputResults/${outputId}`;
    }

    static exportIssueList(issueListId) {
        return serverContextPath + WebApiCls.issueUrl + `/issue_lists/${issueListId}/export`;
    }
}
/**
 * Class that overrides the original WebApiCls and replaces them with methods,
 * that postpone requests, when user is not logged in (unauthorized)
 */
export class WebApiOverride extends WebApiCls {
    callbacks: any[];
    origMethodNames: string[];

    constructor() {
        super();
        this.callbacks = [];
        // get all method names from WebApiCls
        this.origMethodNames = Object.getOwnPropertyNames(WebApiCls.prototype);
        this.overrideMethods();
    }
    /**
     * Overrides the old WebApi methods with new
     */
    overrideMethods() {
        const { origMethodNames } = this;

        for (const i in origMethodNames) {
            const methodName = origMethodNames[i];
            const origMethod = this[methodName];

            this[methodName] = (...args) => {
                return this.newMethod(origMethod, args);
            };
        }
    }
    /**
     * Creates new WebApi method, which postpones the requests that failed, due to user being unauthorized
     */
    newMethod(origMethod, args) {
        return new Promise((resolve, reject) => {
            origMethod
                .call(this, ...args)
                .then(json => {
                    resolve(json);
                })
                .catch(err => {
                    if (err.unauthorized) {
                        this.callbacks.push(() => {
                            origMethod
                                .call(this, ...args)
                                .then(resolve)
                                .catch(reject);
                        });
                    } else {
                        reject(err);
                    }
                });
        });
    }
    /**
     * Repeats all postponed requests
     */
    onLogin() {
        if (this.callbacks && this.callbacks.length > 0) {
            this.callbacks.forEach(callback => callback());
            this.callbacks = [];
        }
    }
}

export const WebApi = new WebApiOverride();
export const _WebApi = new WebApiCls();

// export default {
//     WebApi: new WebApi(),
//     WebApiCls: WebApi,;
//     UrlFactory: UrlFactory,
// };
