// @ts-ignore
import AjaxUtils from '../components/AjaxUtils';
import {CoordinateFileType, DEFAULT_LIST_SIZE, JAVA_ATTR_CLASS} from '../constants';
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
import {ApAccessPointCreateVO} from '../api/ApAccessPointCreateVO';
import {ApAccessPointVO} from '../api/ApAccessPointVO';
import {ApValidationErrorsVO} from '../api/ApValidationErrorsVO';
import {ApStateHistoryVO} from '../api/ApStateHistoryVO';
import {ApAttributesInfoVO} from '../api/ApAttributesInfoVO';
import {ApPartFormVO} from '../api/ApPartFormVO';
import {ApTypeVO} from '../api/ApTypeVO';
import {RulDataTypeVO} from '../api/RulDataTypeVO';
import {RulDescItemTypeExtVO} from '../api/RulDescItemTypeExtVO';
import {RulPartTypeVO} from '../api/RulPartTypeVO';
import {FilteredResultVO} from '../api/FilteredResultVO';
import {ApSearchType} from '../typings/globals';
import * as UrlBuilder from '../utils/UrlBuilder';
import {ArchiveEntityResultListVO} from '../api/ArchiveEntityResultListVO';
import {SearchFilterVO} from 'api/SearchFilterVO';
import {SyncsFilterVO} from '../api/SyncsFilterVO';
import {ExtSyncsQueueResultListVO} from '../api/ExtSyncsQueueResultListVO';
import {ApViewSettings} from '../api/ApViewSettings';
import {UsrUserVO} from '../api/UsrUserVO';

// @ts-ignore
const serverContextPath = window.serverContextPath;

function getData(data, timeout = 1000) {
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
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
            resolve();
        }
    });
}

/**
 * Web api pro komunikaci se serverem.
 */
export class WebApiCls {
    static baseUrl = '/api';
    static v1 = WebApiCls.baseUrl + '/v1';
    static fundV1 = WebApiCls.v1 + '/fund';
    static authUrl = WebApiCls.baseUrl + '/auth';
    static arrangementUrl = WebApiCls.baseUrl + '/arrangement';
    static issueUrl = WebApiCls.baseUrl + '/issue';
    static registryUrl = WebApiCls.baseUrl + '/registry';
    static apUrl = WebApiCls.registryUrl;
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

    /**
     * Seznam entit pro SSO přihlašování.

     * @return seznam sso entit
     */
    getSsoEntities() {
        return AjaxUtils.ajaxGet(WebApiCls.authUrl + '/sso');
    }

    findInFundTree(versionId, nodeId, searchText, type, searchParams = null, luceneQuery = false) {
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

    selectNode(nodeUuid) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/selectNode/' + nodeUuid);
    }

    syncDaoLink(fundVersionId, nodeId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/daos/' + fundVersionId + '/nodes/' + nodeId + '/sync');
    }

    syncDaosByFund(fundVersionId) {
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
    fundFulltextNodes(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + `/fundFulltext/${fundId}`, {fundId});
    }

    getFundsByVersionIds(versionIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getVersions', null, {ids: versionIds});
    }

    getNode(fundVersionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodeInfo/' + fundVersionId + '/' + nodeId);
    }

    getNodes(versionId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodes', null, {versionId: versionId, ids: nodeIds});
    }

    findNodeByIds(fundId, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.adminUrl + '/' + fundId + '/nodes/byIds', null, nodeIds);
    }

    copyOlderSiblingAttribute(versionId, nodeId, nodeVersionId, descItemTypeId) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/copyOlderSiblingAttribute/',
            {versionId, descItemTypeId},
            {id: nodeId, version: nodeVersionId},
        );
    }

    findChanges(versionId, nodeId, offset, maxSize, changeId) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId, {nodeId, offset, maxSize, changeId});
    }

    findChangesByDate(versionId, nodeId, changeId, fromDate) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + '/date', {
            nodeId,
            maxSize: 1,
            changeId,
            fromDate,
        });
    }

    revertChanges(versionId, nodeId, fromChangeId, toChangeId) {
        return AjaxUtils.ajaxGet(WebApiCls.changesUrl + '/' + versionId + '/revert', {
            nodeId,
            fromChangeId,
            toChangeId,
        });
    }

    validateUnitdate(value) {
        return AjaxUtils.ajaxGet(WebApiCls.validateUrl + '/unitDate', {value: value || ''});
    }

    moveNodesUnder(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelUnder', null, data);
    }

    moveNodesBefore(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId: versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelBefore', null, data);
    }

    moveNodesAfter(versionId, nodes, nodesParent, dest, destParent) {
        const data = {
            versionId,
            transportNodes: nodes,
            transportNodeParent: nodesParent,
            staticNode: dest,
            staticNodeParent: destParent,
        };
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/moveLevelAfter', null, data);
    }

    createDescItem(versionId, nodeId, nodeVersionId, descItemTypeId, descItem) {
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

    createOutputItem(versionId, getOutputId, outputVersion, descItemTypeId, descItem) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
                '/outputItems/' +
                versionId +
                '/' +
                getOutputId +
                '/' +
                outputVersion +
                '/' +
                descItemTypeId +
                '/create',
            null,
            descItem,
        );
    }

    updateDescItem(versionId, nodeId, nodeVersionId, descItem) {
        return callWS(
            '/arrangement/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/update/true',
            descItem,
        );

        // Původní volání kontroleru - zatím necháno pro testovací účely
        // return AjaxUtils.ajaxPut(WebApi.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/update/true', null,  descItem);
    }

    updateDescItems(
        fundVersionId,
        nodeId,
        nodeVersionId,
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

    setNotIdentifiedDescItem(versionId, nodeId, parentNodeVersion, descItemTypeId, descItemSpecId, descItemObjectId) {
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
            {descItemTypeId, descItemSpecId, descItemObjectId},
        );
    }

    unsetNotIdentifiedDescItem(versionId, nodeId, parentNodeVersion, descItemTypeId, descItemSpecId, descItemObjectId) {
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
            {descItemTypeId, descItemSpecId, descItemObjectId},
        );
    }

    updateOutputItem(versionId, outputVersion, descItem) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputVersion + '/update/true',
            null,
            descItem,
        );
    }

    deleteDescItem(versionId, nodeId, nodeVersionId, descItem) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/descItems/' + versionId + '/' + nodeId + '/' + nodeVersionId + '/delete',
            null,
            descItem,
        );
    }

    deleteOutputItem(versionId, outputVersion, parentVersionId, descItemObjectId) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/' + outputVersion + '/delete',
            null,
            descItemObjectId,
        );
    }

    deleteDescItemType(versionId, nodeId, nodeVersionId, descItemTypeId) {
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

    deleteOutputItemType(versionId, getOutputId, outputVersion, descItemTypeId) {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl +
                '/outputItems/' +
                versionId +
                '/' +
                getOutputId +
                '/' +
                outputVersion +
                '/' +
                descItemTypeId,
            null,
            null,
        );
    }

    setNotIdentifiedOutputItem(
        versionId,
        getOutputId,
        outputVersion,
        outputItemTypeId,
        outputItemSpecId,
        outputItemObjectId,
    ) {
        //return callWS('/arrangement/outputItems/' + versionId + '/' + getOutputId + '/' + outputVersion + '/notUndefined/set?outputItemTypeId=' + outputItemTypeId + '&outputItemSpecId=' + outputItemSpecId + '&outputItemObjectId=' + outputItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
                '/outputItems/' +
                versionId +
                '/' +
                getOutputId +
                '/' +
                outputVersion +
                '/notUndefined/set',
            {outputItemTypeId, outputItemSpecId, outputItemObjectId},
        );
    }

    unsetNotIdentifiedOutputItem(
        versionId,
        getOutputId,
        outputVersion,
        outputItemTypeId,
        outputItemSpecId,
        outputItemObjectId,
    ) {
        //return callWS('/arrangement/outputItems/' + versionId + '/' + getOutputId + '/' + outputVersion + '/notUndefined/unset?outputItemTypeId=' + outputItemTypeId + '&outputItemSpecId=' + outputItemSpecId + '&outputItemObjectId=' + outputItemObjectId, null);

        // Původní volání kontroleru - zatím necháno pro testovací účely
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl +
                '/outputItems/' +
                versionId +
                '/' +
                getOutputId +
                '/' +
                outputVersion +
                '/notUndefined/unset',
            {outputItemTypeId, outputItemSpecId, outputItemObjectId},
        );
    }

    switchOutputCalculating(fundVersionId, getOutputId, itemTypeId, strict) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + getOutputId + '/' + fundVersionId + '/' + itemTypeId + '/switch',
            {strict},
            null,
        );
    }

    updateOutputSettings(outputId, outputSettings) {
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
    addOutputTemplate(outputId:number, templateId:number):Promise<void> {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + '/template/' + templateId, null, null);
    }

    /**
     * Odebrání omezujícího rejstříku z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param templateId identifikátor rejstříku
     */
    deleteOutputTemplate(outputId:number, templateId:number):Promise<void> {
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
    addRestrictedScope(outputId, scopeId) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + outputId + '/restrict/' + scopeId, null, null);
    }

    /**
     * Odebrání omezujícího rejstříku z výstupu
     *
     * @param outputId identifikátor výstupu
     * @param scopeId identifikátor rejstříku
     */
    deleteRestrictedScope(outputId, scopeId) {
        return AjaxUtils.ajaxDelete(
            WebApiCls.arrangementUrl + '/output/' + outputId + '/restrict/' + scopeId,
            null,
            null,
        );
    }

    addNode(node, parentNode, versionId, direction, descItemCopyTypes, scenarioName, createItems, count = 1) {
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
        targetFundVersionId,
        sourceFundVersionId,
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
        targetFundVersionId,
        targetStaticNode,
        targetStaticNodeParent,
        sourceFundVersionId,
        sourceNodes,
        ignoreRootNodes = false,
        selectedDirection,
        filesConflictResolve = null,
        structuresConflictResolve = null,
        templateId = null,
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

    getNodeAddScenarios(node, versionId, direction, withGroups = false) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/scenarios',
            {withGroups: withGroups},
            {
                versionId,
                direction,
                node,
            },
        );
    }

    getBulkActions(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/' + versionId, null);
    }

    getBulkActionsState(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/states/' + versionId, null);
    }

    getBulkActionsList(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/list/' + versionId, null);
    }

    bulkActionValidate(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/validate/' + versionId, null);
    }

    getBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId, null);
    }

    interruptBulkAction(bulkActionRunId) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/action/' + bulkActionRunId + '/interrupt', null);
    }

    queueBulkAction(versionId, code) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null);
    }

    queueBulkActionWithIds(versionId, code, nodeIds) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/' + versionId + '/' + code, null, nodeIds);
    }

    queuePersistentSortByIds(versionId, code, nodeIds, config) {
        return AjaxUtils.ajaxPost(WebApiCls.actionUrl + '/queue/persistentSort/' + versionId + '/' + code, null, {
            nodeIds,
            ...config,
        });
    }

    versionValidate(versionId, showAll = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersion/' + versionId + '/' + showAll, null);
    }

    versionValidateCount(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/validateVersionCount/' + versionId, null);
    }

    getFundPolicy(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/fund/policy/' + fundVersionId, {});
    }

    resetServerCache() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/cache/reset', {});
    }

    /// Registry
    createAccessPoint(accessPoint: ApAccessPointCreateVO): Promise<ApAccessPointVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/', null, accessPoint);
    }

	getStateApproval(accessPointId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/nextStates');
	}

    getStateApprovalRevision(accessPointId) {
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
        searchFilter?: SearchFilterVO,
    ): Promise<FilteredResultVO<ApAccessPointVO>> {
        return AjaxUtils.ajaxPost(
            WebApiCls.registryUrl + '/search',
            {
                search,
                from,
                count,
                itemTypeId,
                itemSpecId,
                parentRecordId: registryParent,
                apTypeId,
                versionId,
                scopeId,
                excludeInvalid,
                state,
                searchTypeName,
                searchTypeUsername,
                revState,
            },
            searchFilter,
        );
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
        filter: SearchFilterVO,
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
        filter: SearchFilterVO,
    ): Promise<ArchiveEntityResultListVO> {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/external/search', {from, max, externalSystemCode}, filter);
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
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/external/syncs', {from, max, externalSystemCode}, filter);
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
        return AjaxUtils.ajaxPost(url, {scopeId, externalSystemCode});
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
        return AjaxUtils.ajaxPost(url, {externalSystemCode, replace});
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
        return AjaxUtils.ajaxPost(url, {externalSystemCode});
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
        return AjaxUtils.ajaxPost(url, {externalSystemCode});
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
        return AjaxUtils.ajaxPost(url, {externalSystemCode});
    }

    disconnectAccessPoint(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/disconnect/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, {externalSystemCode});
    }

    takeRelArchiveEntities(accessPointId: number, externalSystemCode: string) {
        const url = UrlBuilder.bindParams(WebApiCls.registryUrl + '/external/take-rel/{accessPointId}', {
            accessPointId,
        });
        return AjaxUtils.ajaxPost(url, {externalSystemCode});
    }

    getApTypeViewSettings(): Promise<ApViewSettings> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/ap-types/view-settings');
    }

    findRegistryUsage(recordId) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + recordId + '/usage');
    }

    getAccessPoint(accessPointId): Promise<ApAccessPointVO> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId);
    }

    findStateHistories(accessPointId: number): Promise<ApStateHistoryVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/' + accessPointId + '/history');
    }

    updateAccessPoint(accessPointId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId, null, data);
    }

    changeDescription(accessPointId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/' + accessPointId + '/description', null, data);
    }

    replaceRegistry(recordReplaceId, recordReplacementId) {
        return AjaxUtils.ajaxPost(
            WebApiCls.registryUrl + '/' + recordReplaceId + '/replace',
            null,
            recordReplacementId,
        );
    }

    getScopes(versionId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/fundScopes', {versionId});
    }

    getAllScopes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/scopes', null);
    }

    getScopeWithConnected(scopeId = null) {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/scopes/' + scopeId + '/withConnected', null);
    }

    createScope() {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/scopes', null);
    }

    updateScope(scopeId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.registryUrl + '/scopes/' + scopeId, null, data);
    }

    deleteScope(scopeId) {
        return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/scopes/' + scopeId, null);
    }

    connectScope(scopeId, connectedScopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/scopes/' + scopeId + '/connect', null, connectedScopeId);
    }

    disconnectScope(scopeId, connectedScopeId) {
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
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/part', { apVersion: apVersion ? apVersion - 1 : undefined }, apPartFormVO);
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
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/' + accessPointId + '/part/' + partId, { apVersion }, apPartFormVO);
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
        return AjaxUtils.ajaxPost(WebApiCls.registryUrl + '/revision/' + accessPointId + '/part/' + partId, { apVersion }, apPartFormVO);
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
     * Validace přístupového bodu
     *
     * @param accessPointId identifikátor přístupového bodu (PK)
     * @return validační chyby přístupového bodu
     */
    validateAccessPoint(accessPointId: number, includeRevision?: boolean): Promise<ApValidationErrorsVO> {
        return AjaxUtils.ajaxGet(`${WebApiCls.registryUrl}/${accessPointId}/validate${includeRevision ? '?includeRevision=true' : ''}`);
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

    findPartTypes(): Promise<RulPartTypeVO[]> {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/part-type');
    }

    // End registry

    getFundNodeForm(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + nodeId + '/' + versionId + '/form');
    }

    getOutputNodeForm(versionId, getOutputId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + getOutputId + '/' + versionId + '/form');
    }

    getFundNodeForms(versionId, nodeIds) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + versionId + '/forms', {nodeIds: nodeIds});
    }

    getFundNodeFormsWithAround(versionId, nodeId, around) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/nodes/' + versionId + '/' + nodeId + '/' + around + '/forms',
        );
    }

    getFundNodeRegister(versionId, nodeId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/registerLinks/' + nodeId + '/' + versionId + '/form');
    }

    getFundNodeDaos(versionId, nodeId = null, detail = false, from = 0, max = 10000) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daos/' + versionId, {
            nodeId,
            detail,
            index: from,
            maxResults: max,
        });
    }

    findDaoPackages(versionId, search, unassigned) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/daopackages/' + versionId, {search, unassigned});
    }

    getPackageDaos(versionId, daoPackageId, unassigned, detail = false, from = 0, max = 10000) {
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

    getGroups(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/groups/' + fundVersionId);
    }

    getTemplates(code = null) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/templates', code ? {code} : null);
    }

    getRequestsInQueue() {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/queued');
    }

    findRequests(versionId, type, state, description, fromDate, toDate, subType) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId, {
            state,
            type,
            description,
            fromDate,
            toDate,
            subType,
        });
    }

    arrDigitizationRequestAddNodes(versionId, reqId, send, description, nodeIds, digitizationFrontdeskId) {
        const data = {
            id: reqId,
            nodeIds,
            description,
            digitizationFrontdeskId,
        };
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/requests/' + versionId + '/digitization/add',
            {send},
            data,
        );
    }

    arrDaoRequestAddDaos(versionId, reqId, send, description, daoIds, type) {
        const data = {
            id: reqId,
            daoIds,
            description,
            type,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/dao/add', {send}, data);
    }

    arrRequestRemoveNodes(versionId, reqId, nodeIds) {
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

    updateArrRequest(versionId, id, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id, null, data);
    }

    removeArrRequestQueueItem(id) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getArrRequest(versionId, id) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id, {detail: true});
    }

    sendArrRequest(versionId, id) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/requests/' + versionId + '/' + id + '/send');
    }

    deleteArrRequest(versionId, id) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/requests/' + id);
    }

    getFundTree(versionId, nodeId, expandedIds = {}, includeIds = []) {
        const data = {
            versionId,
            nodeId,
            includeIds,
            expandedIds: expandedIds ? Object.keys(expandedIds) : [],
        };

        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/fundTree', null, data);
    }

    getNodeData(fundVersionId, nodeParam, resultParam: any | object = {}) {
        const data = {
            fundVersionId: fundVersionId,
            nodeId: nodeParam.nodeId,
            nodeIndex: nodeParam.nodeIndex,
            parentNodeId: nodeParam.parentNodeId,
            formData: resultParam.formData,
            siblingsFrom: resultParam.siblingsFrom,
            siblingsMaxCount: resultParam.siblingsMaxCount,
            siblingsFilter: resultParam.siblingsFilter,
            parents: resultParam.parents,
            children: resultParam.children,
        };
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/nodeData', null, data);
    }

    getFundTreeNodes(versionId, nodeIds) {
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
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/logs', {lineCount, firstLine});
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

    approveVersion(versionId) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/approveVersion', {versionId});
    }

    filterNodes(versionId, filter) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/filterNodes/' + versionId, {}, filter);
    }

    getFilteredNodes(versionId, pageIndex, pageSize, descItemTypeIds) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/getFilterNodes/' + versionId,
            {page: pageIndex, pageSize: pageSize},
            descItemTypeIds,
        );
    }

    replaceDataValues(versionId, descItemTypeId, specsIds, searchText, replaceText, nodes, selectionType) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/replaceDataValues/' + versionId,
            {descItemTypeId, searchText, replaceText},
            {nodes, specIds: specsIds, selectionType},
        );
    }

    placeDataValues(versionId, descItemTypeId, specsIds, replaceText, replaceSpecId, nodes, selectionType, append = false) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/placeDataValues/' + versionId + (append ? '?append=true' : ""),
            {descItemTypeId, newDescItemSpecId: replaceSpecId, text: replaceText},
            {nodes, specIds: specsIds, selectionType},
        );
    }

    setSpecification(fundVersionId, itemTypeId, specIds, replaceSpecId, nodes, selectionType) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/setSpecification/' + fundVersionId,
            {itemTypeId, replaceSpecId},
            {nodes, specIds, selectionType},
        );
    }

    setDataValues(fundVersionId, itemTypeId, specIds, replaceValueId, nodes, selectionType, valueIds) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/setDataValues/' + fundVersionId,
            {itemTypeId, replaceValueId},
            {nodes, specIds, selectionType, valueIds},
        );
    }

    deleteDataValues(versionId, descItemTypeId, specsIds, nodes, selectionType, valueIds) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/deleteDataValues/' + versionId,
            {descItemTypeId},
            {nodes, specIds: specsIds, selectionType, valueIds},
        );
    }

    getFilteredFulltextNodes(versionId, fulltext, luceneQuery = false, searchParams = null) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/getFilteredFulltext/' + versionId, null, {
            fulltext,
            luceneQuery,
            searchParams,
        });
    }

    getPackages() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/getPackages');
    }

    deletePackage(code) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/deletePackage/' + code);
    }

    createDaoLink(versionId, daoId, nodeId) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/daos/' + versionId + '/' + daoId + '/' + nodeId + '/create',
            null,
            null,
        );
    }

    deleteDaoLink(versionId, daoLinkId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/daolinks/' + versionId + '/' + daoLinkId, null, null);
    }

    /**
     * Získání odkazovaných JP.
     *
     * @param fundVersionId verze AS
     * @param nodeId        JP pro kterou zjišťujeme odkazované JP
     * @return seznam JP
     */
    findLinkedNodes(fundVersionId, nodeId) {
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
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/nodes/' + nodeId + '/' + nodeVersion + '/sync/', {
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

    arrCoordinatesImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);

        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/descCoordinates', {}, 'POST', formData);
    }

    arrOutputCoordinatesImport(versionId, getOutputId, outputVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('fundVersionId', versionId);
        formData.append('descItemTypeId', descItemTypeId);
        formData.append('getOutputId', getOutputId);
        formData.append('outputVersion', outputVersion);

        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/outputCoordinates', {}, 'POST', formData);
    }

    regCoordinatesImport(data) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.kmlUrl + '/import/regCoordinates', {}, 'POST', data);
    }

    descItemCsvImport(versionId, nodeId, nodeVersionId, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('nodeId', nodeId);
        formData.append('nodeVersion', nodeVersionId);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(
            WebApiCls.arrangementUrl + '/descItems/' + versionId + '/csv/import',
            {},
            'POST',
            formData,
        );
    }

    descOutputItemCsvImport(versionId, getOutputId, outputVersion, descItemTypeId, file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('getOutputId', getOutputId);
        formData.append('outputVersion', outputVersion);
        formData.append('descItemTypeId', descItemTypeId);

        return AjaxUtils.ajaxCallRaw(
            WebApiCls.arrangementUrl + '/outputItems/' + versionId + '/csv/import',
            {},
            'POST',
            formData,
        );
    }

    getInstitutions(hasFund = null) {
        return AjaxUtils.ajaxGet(WebApiCls.partyUrl + '/institutions', {hasFund});
    }

    /**
     * Hledá všechny unikátní hodnoty atributu pro daný AS
     */
    getDescItemTypeValues(versionId, descItemTypeId, fulltext, descItemSpecIds, max) {
        return AjaxUtils.ajaxPut(
            WebApiCls.arrangementUrl + '/filterUniqueValues/' + versionId,
            {descItemTypeId, fulltext, max},
            descItemSpecIds,
        );
    }

    findUniqueSpecIds(fundVersionId, itemTypeId, filters) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/findUniqueSpecIds/' + fundVersionId,
            {itemTypeId},
            filters,
        );
    }

    getVisiblePolicy(nodeId, fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/' + nodeId + '/' + fundVersionId);
    }

    getVisiblePolicyTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.ruleUrl + '/policy/types');
    }

    setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree = false, nodeExtensions) {
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

    login(username, password) {
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
    findFunds(fulltext, max = DEFAULT_LIST_SIZE, from = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFunds', {fulltext, max, from}).then(json => ({
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
        }).then(json => ({data: json.rows, count: json.count}));
    }

    findControlFunds(fulltext: string, max:number = DEFAULT_LIST_SIZE, from: number = 0) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/controlFunds', {search: fulltext, from, count: max});
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
        }).then(json => ({data: json.rows, count: json.count}));
    }

    findUsersPermissionsByFund(fundId) {
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

    findGroupsPermissionsByFund(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/${fundId}/groups`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    findGroupsPermissionsByFundAll(fundId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + `/fund/all/groups`).then(data => ({
            rows: data,
            count: data.length,
        }));
    }

    changeUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission', null, permissions);
    }

    addUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/add', null, permissions);
    }

    addGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/add', null, permissions);
    }

    deleteUserPermission(userId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete', null, permissions);
    }

    deleteGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete', null, permissions);
    }

    deleteUserFundPermission(userId, fundId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/fund/' + fundId);
    }

    deleteUserFundAllPermission(userId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/fund/all');
    }

    deleteGroupFundPermission(groupId, fundId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/fund/' + fundId);
    }

    deleteGroupFundAllPermission(groupId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/fund/all');
    }

    deleteUserScopePermission(userId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/' + userId + '/permission/delete/scope/' + scopeId);
    }

    deleteGroupScopePermission(groupId, scopeId) {
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl + '/' + groupId + '/permission/delete/scope/' + scopeId);
    }

    changeGroupPermission(groupId, permissions) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/' + groupId + '/permission', null, permissions);
    }

    findGroup(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl, {search: fulltext, from: 0, count: max}).then(json => ({
            groups: json.rows,
            groupsCount: json.count,
        }));
    }

    findGroupWithFundCreate(fulltext, max = DEFAULT_LIST_SIZE) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + '/withFundCreate', {
            search: fulltext,
            from: 0,
            count: max,
        }).then(json => ({groups: json.rows, groupsCount: json.count}));
    }

    getUser(userId) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId);
    }

    getUserOld(userId) {
        return AjaxUtils.ajaxGet(WebApiCls.userUrl + '/' + userId + '/old');
    }

    createGroup(name, code, description) {
        const params = {
            name: name,
            code: code,
            description,
        };
        return AjaxUtils.ajaxPost(WebApiCls.groupUrl, null, params);
    }

    updateGroup(groupId, name, description) {
        return AjaxUtils.ajaxPut(WebApiCls.groupUrl + '/' + groupId, null, {name, description});
    }

    deleteGroup(groupId) {
        return AjaxUtils.ajaxDelete(WebApiCls.groupUrl + '/' + groupId);
    }

    joinGroup(groupIds, userIds) {
        const data = {
            groupIds: groupIds,
            userIds: userIds,
        };
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/join/', null, data);
    }

    leaveGroup(groupId, userId) {
        return AjaxUtils.ajaxPost(WebApiCls.userUrl + '/group/' + groupId + '/leave/' + userId, null, null);
    }

    createUser(username, valuesMap, accessPointId) {
        const params = {
            username: username,
            valuesMap: valuesMap,
            accessPointId: accessPointId,
        };
        return AjaxUtils.ajaxPost(WebApiCls.userUrl, null, params);
    }

    updateUser(id, accessPointId, username, valuesMap) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + id, null, {accessPointId, username, valuesMap});
    }

    changePasswordUser(oldPassword, newPassword) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/password', null, {oldPassword, newPassword});
    }

    changePassword(userId, newPassword) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/password', null, {newPassword});
    }

    changeActive(userId, active) {
        return AjaxUtils.ajaxPut(WebApiCls.userUrl + '/' + userId + '/active/' + active);
    }

    getGroup(groupId) {
        return AjaxUtils.ajaxGet(WebApiCls.groupUrl + '/' + groupId);
    }

    getFundDetail(fundId) {
        console.warn("11111111111", fundId);
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/getFund/' + fundId).then(json => {
            return {
                ...json,
                versionId: json.versions[0].id,
                activeVersion: json.versions[0],
            };
        });
    }

    getValidationItems(fundVersionId, fromIndex, toIndex) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/' + fromIndex + '/' + toIndex,
        );
    }

    findValidationError(fundVersionId, nodeId, direction) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/validation/' + fundVersionId + '/find/' + nodeId + '/' + direction,
        );
    }

    deleteFund(fundId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/deleteFund/' + fundId);
    }

    deleteFundHistory(fundId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/deleteFundHistory/' + fundId);
    }

    getOutputTypes(versionId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/types/' + versionId);
    }

    getOutputs(versionId, state) {
        return AjaxUtils.ajaxGet(
            WebApiCls.arrangementUrl + '/output/' + versionId + (state != null ? '?state=' + state : ''),
        );
    }

    getFundOutputDetail(versionId, outputId) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createOutput(versionId, data) {
        return AjaxUtils.ajaxPut(WebApiCls.arrangementUrl + '/output/' + versionId, null, data);
    }

    updateOutput(versionId, outputId, data) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/update',
            null,
            data,
        );
    }

    outputUsageEnd(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/lock');
    }

    fundOutputAddNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/add',
            null,
            nodeIds,
        );
    }

    fundOutputRemoveNodes(versionId, outputId, nodeIds) {
        return AjaxUtils.ajaxPost(
            WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/remove',
            null,
            nodeIds,
        );
    }

    outputDelete(versionId, outputId) {
        return AjaxUtils.ajaxDelete(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId);
    }

    createFundFileRaw(formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund/', {}, 'POST', formData);
    }

    createFundFile(formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund', null, formData);
    }

    getMimeTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.attachmentUrl + '/mimeTypes', null);
    }

    findFundFiles(fundId, searchText, count = 20) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId, {count: count, search: searchText});
    }

    getEditableFundFile(fundId, fileId) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/fund/' + fundId + '/' + fileId);
    }

    updateFundFileRaw(fileId, formData) {
        return AjaxUtils.ajaxCallRaw(WebApiCls.dmsUrl + '/fund/' + fileId, {}, 'POST', formData);
    }

    updateFundFile(fileId, formData) {
        return AjaxUtils.ajaxPost(WebApiCls.dmsUrl + '/fund/' + fileId, null, formData);
    }

    deleteArrFile(fileId) {
        return AjaxUtils.ajaxDelete(WebApiCls.dmsUrl + '/fund/' + fileId, null, null);
    }

    findFundOutputFiles(outputId) {
        return AjaxUtils.ajaxGet(WebApiCls.dmsUrl + '/output/' + outputId);
    }

    getFundOutputFunctions(outputId, getRecommended) {
        return AjaxUtils.ajaxGet(WebApiCls.actionUrl + '/output/' + outputId, {recommended: getRecommended});
    }

    outputGenerate(outputId, forced = false) {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/generate/' + outputId, {forced});
    }

    outputSend(outputId: number):Promise<void> {
        return AjaxUtils.ajaxGet(WebApiCls.arrangementUrl + '/output/send/' + outputId.toString());
    }

    outputRevert(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/revert');
    }

    outputClone(versionId, outputId) {
        return AjaxUtils.ajaxPost(WebApiCls.arrangementUrl + '/output/' + versionId + '/' + outputId + '/clone');
    }

    getApExternalSystems() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/externalSystems');
    }

    getEidTypes() {
        return AjaxUtils.ajaxGet(WebApiCls.registryUrl + '/eidTypes');
    }

    getAllExtSystem() {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems', null);
    }

    getExtSystem(id) {
        return AjaxUtils.ajaxGet(WebApiCls.adminUrl + '/externalSystems/' + id, null);
    }

    createExtSystem(extSystem) {
        return AjaxUtils.ajaxPost(WebApiCls.adminUrl + '/externalSystems', null, extSystem);
    }

    updateExtSystem(id, extSystem) {
        return AjaxUtils.ajaxPut(WebApiCls.adminUrl + '/externalSystems/' + id, null, extSystem);
    }

    deleteExtSystem(id) {
        return AjaxUtils.ajaxDelete(WebApiCls.adminUrl + '/externalSystems/' + id, null);
    }

	deleteExtSyncsQueueItem(itemId) {
		return AjaxUtils.ajaxDelete(WebApiCls.registryUrl + '/external/syncs/' + itemId, null);
	}

    findFundStructureExtension(fundVersionId, structureTypeCode) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode);
    }

    updateFundStructureExtension(fundVersionId, structureTypeCode, structureExtensionCodes) {
        return AjaxUtils.ajaxPut(
            WebApiCls.structureUrl + '/extension/' + fundVersionId + '/' + structureTypeCode,
            null,
            structureExtensionCodes,
        );
    }

    findRulStructureTypes(fundVersionId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/type', {fundVersionId});
    }

    getStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    findStructureData(
        fundVersionId: number,
        structureTypeCode,
        search:string | null = null,
        assignable:boolean = true,
        from:number = 0,
        count:number = DEFAULT_LIST_SIZE,
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

    createStructureData(fundVersionId, structureTypeCode, value = null) {
        // Kvůli JSON stringify musíme poslat pomocí RAW aby se nevytvořili '"' v body
        return AjaxUtils.ajaxCallRaw(
            WebApiCls.structureUrl + '/data/' + fundVersionId,
            {value},
            'POST',
            structureTypeCode,
            'application/json',
        );
    }

    duplicateStructureDataBatch(fundVersionId, structureDataId, data) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/batch',
            null,
            data,
        );
    }

    confirmStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId + '/confirm',
        );
    }

    deleteStructureData(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxDelete(WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureDataId);
    }

    getFormStructureItems(fundVersionId, structureDataId) {
        return AjaxUtils.ajaxGet(WebApiCls.structureUrl + '/item/form/' + fundVersionId + '/' + structureDataId);
    }

    createStructureItem(fundVersionId, structureDataId, itemTypeId, data) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/item/' + fundVersionId + '/' + structureDataId + '/' + itemTypeId + '/create',
            null,
            data,
        );
    }

    updateStructureItem(fundVersionId, data, createNewVersion = true) {
        return AjaxUtils.ajaxPut(
            WebApiCls.structureUrl + '/item/' + fundVersionId + '/update/' + createNewVersion,
            null,
            data,
        );
    }

    deleteStructureItem(fundVersionId, data) {
        return AjaxUtils.ajaxPost(WebApiCls.structureUrl + '/item/' + fundVersionId + '/delete', null, data);
    }

    deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId) {
        return AjaxUtils.ajaxDelete(
            WebApiCls.structureUrl + '/item/' + fundVersionId + '/' + structureDataId + '/' + itemTypeId,
        );
    }

    updateStructureDataBatch(fundVersionId, structureTypeCode, structureDataBatchUpdate) {
        return AjaxUtils.ajaxPost(
            WebApiCls.structureUrl + '/data/' + fundVersionId + '/' + structureTypeCode + '/batchUpdate',
            null,
            structureDataBatchUpdate,
        );
    }

    setAssignableStructureDataList(fundVersionId, assignable, structureDataIds) {
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
        return AjaxUtils.ajaxGet(WebApiCls.issueUrl + '/funds/' + fundId + '/issue_lists', {open});
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

    static downloadOutputResults(outputId:number) {
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
        const {origMethodNames} = this;

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
